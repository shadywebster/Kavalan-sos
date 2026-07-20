package com.example.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.telephony.SmsManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AudioRecord
import com.example.data.EmergencyContact
import com.example.data.KavalanRepository
import com.example.data.PoliceStation
import com.example.data.SafetyTip
import com.example.util.AudioRecorder
import com.example.util.GpsCoordinates
import com.example.util.LocationHelper
import com.example.util.SirenPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class KavalanViewModel(
    private val repository: KavalanRepository,
    private val app: Application
) : AndroidViewModel(app) {

    // Database state flows
    val allContacts: StateFlow<List<EmergencyContact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTips: StateFlow<List<SafetyTip>> = repository.allTips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<AudioRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStations: StateFlow<List<PoliceStation>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    val gpsLocation = MutableStateFlow<GpsCoordinates?>(null)
    val isRecording = MutableStateFlow(false)
    val isSirenPlaying = MutableStateFlow(false)
    val sosStatus = MutableStateFlow<String?>(null) // "Sending...", "Alert Sent!", "Failed to locate"
    val playingRecordId = MutableStateFlow<Int?>(null)
    
    // Emergency SIM slot option (0 = System Default/Ask, 1 = SIM Slot 1, 2 = SIM Slot 2)
    val preferredSimSlot = MutableStateFlow(0)
    // Siren and buzzer sound option ("Buzzer & Siren", "Industrial Buzzer", "Police Siren")
    val sirenSoundType = MutableStateFlow("Buzzer & Siren")

    // Helpers
    private val sirenPlayer = SirenPlayer()
    private val audioRecorder = AudioRecorder(app)
    private var mediaPlayer: MediaPlayer? = null

    init {
        // Fetch initial GPS location
        refreshLocation()
    }

    fun refreshLocation() {
        LocationHelper.getCurrentLocation(app) { coords ->
            gpsLocation.value = coords
        }
    }

    // Siren playback management
    fun toggleSiren() {
        if (sirenPlayer.isPlaying()) {
            sirenPlayer.stop()
            isSirenPlaying.value = false
        } else {
            sirenPlayer.start(viewModelScope, sirenSoundType.value)
            isSirenPlaying.value = true
        }
    }

    fun stopSirenIfPlaying() {
        if (sirenPlayer.isPlaying()) {
            sirenPlayer.stop()
            isSirenPlaying.value = false
        }
    }

    // Evidence Audio Recording management
    fun startVoiceRecording() {
        if (isRecording.value) return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "KAVALAN_REC_$timeStamp.mp4"
        val file = audioRecorder.start(fileName)
        if (file != null) {
            isRecording.value = true
        } else {
            Toast.makeText(app, "Failed to start microphone recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceRecording() {
        if (!isRecording.value) return
        val result = audioRecorder.stop()
        isRecording.value = false
        if (result != null) {
            val (file, duration) = result
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertRecord(
                    AudioRecord(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = duration
                    )
                )
            }
            Toast.makeText(app, "Voice evidence saved securely!", Toast.LENGTH_SHORT).show()
        }
    }

    // Playback of evidence logs
    fun playAudioRecord(record: AudioRecord) {
        if (playingRecordId.value == record.id) {
            stopAudioPlayback()
            return
        }
        stopAudioPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(record.filePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopAudioPlayback()
                }
            }
            playingRecordId.value = record.id
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(app, "Failed to play back recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopAudioPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        playingRecordId.value = null
    }

    fun deleteAudioRecord(record: AudioRecord) {
        stopAudioPlayback()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteRecord(record)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // SOS Emergency execution trigger
    fun triggerSosAlert(onCallInitiated: (String) -> Unit) {
        sosStatus.value = "Initiating SOS alerts..."
        
        // Auto-start recording for evidence
        if (!isRecording.value) {
            startVoiceRecording()
        }

        // Fetch location and handle notifications
        LocationHelper.getCurrentLocation(app) { coords ->
            val locationStr = if (coords != null) {
                gpsLocation.value = coords
                "My GPS coordinates are: https://maps.google.com/?q=${coords.latitude},${coords.longitude}"
            } else {
                "Unable to pinpoint exact GPS. Trace my phone connection immediately."
            }

            val smsMessage = "🚨 EMERGENCY SOS! 🚨\nI am in danger! $locationStr\nSent via Kavalan Safety app."
            val contacts = allContacts.value

            if (contacts.isEmpty()) {
                sosStatus.value = "No Guard contacts configured. Calling Police..."
            } else {
                var smsSuccessCount = 0
                for (contact in contacts) {
                    try {
                        @Suppress("DEPRECATION")
                        val smsManager = SmsManager.getDefault()
                        val parts = smsManager.divideMessage(smsMessage)
                        smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                        smsSuccessCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                sosStatus.value = "SMS Alerts dispatched to $smsSuccessCount Guardians!"
            }

            // Always call police helpline as requested (1091 - TN Women Police or 112 / 100)
            // We default to 1091 for TN Women Police, fallback to 112 if unavailable
            val targetPhone = "1091" 
            onCallInitiated(targetPhone)
        }
    }

    // Guardian Contact configuration functions
    fun addContact(name: String, phoneNumber: String, relationship: String) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertContact(
                EmergencyContact(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    relationship = relationship.trim()
                )
            )
        }
    }

    fun removeContact(contact: EmergencyContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sirenPlayer.stop()
        stopAudioPlayback()
    }
}

class KavalanViewModelFactory(
    private val repository: KavalanRepository,
    private val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KavalanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KavalanViewModel(repository, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
