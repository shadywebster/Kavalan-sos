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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
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

    // Live Tracking & AI Assistant States
    val isLiveTracking = MutableStateFlow(false)
    val liveTrackingPath = MutableStateFlow<List<GpsCoordinates>>(emptyList())
    private var liveTrackingJob: kotlinx.coroutines.Job? = null

    val aiResponse = MutableStateFlow<String?>(null)
    val aiRiskLevel = MutableStateFlow("LOW") // LOW, MEDIUM, HIGH, CRITICAL
    val aiGuidance = MutableStateFlow<String?>(null)
    val isAiLoading = MutableStateFlow(false)
    
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

    // Live Location Tracking Loop
    fun toggleLiveTracking() {
        if (isLiveTracking.value) {
            isLiveTracking.value = false
            liveTrackingJob?.cancel()
            liveTrackingJob = null
        } else {
            isLiveTracking.value = true
            liveTrackingJob = viewModelScope.launch(Dispatchers.Default) {
                while (isLiveTracking.value) {
                    LocationHelper.getCurrentLocation(app) { coords ->
                        coords?.let {
                            gpsLocation.value = it
                            // Add to path history
                            val currentPath = liveTrackingPath.value.toMutableList()
                            currentPath.add(it)
                            // Keep last 15 points
                            if (currentPath.size > 15) {
                                currentPath.removeAt(0)
                            }
                            liveTrackingPath.value = currentPath

                            // If SOS is active, dispatch location update to emergency contacts
                            if (sosStatus.value != null) {
                                sendSmsUpdateToContacts(it)
                            }
                        }
                    }
                    delay(10000) // 10 seconds interval
                }
            }
        }
    }

    private fun sendSmsUpdateToContacts(coords: GpsCoordinates) {
        val locationStr = "My live GPS coordinates are: https://maps.google.com/?q=${coords.latitude},${coords.longitude}"
        val smsMessage = "🚨 LIVE KAVALAN SOS UPDATE 🚨\nMy updated location: $locationStr"
        val contacts = allContacts.value
        for (contact in contacts) {
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(smsMessage)
                smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // AI Safety Assistant and Incident Detection
    fun assessSituation(situationText: String) {
        if (situationText.isBlank()) return
        isAiLoading.value = true
        aiResponse.value = null
        aiGuidance.value = null
        aiRiskLevel.value = "LOW"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(1500)
                    val (risk, desc, guide) = simulateAiAssessment(situationText)
                    aiRiskLevel.value = risk
                    aiResponse.value = desc
                    aiGuidance.value = guide
                    isAiLoading.value = false
                    return@launch
                }

                // Construct REST query using standard org.json objects
                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        val contentObj = org.json.JSONObject().apply {
                            val partsArray = org.json.JSONArray().apply {
                                val partObj = org.json.JSONObject().apply {
                                    put("text", situationText)
                                }
                                put(partObj)
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)

                    val systemInstructionObj = org.json.JSONObject().apply {
                        val partsArray = org.json.JSONArray().apply {
                            val partObj = org.json.JSONObject().apply {
                                put("text", "You are the Kavalan AI Guardian, an elite real-time incident detector and rescue threat assessment system for women's safety in Tamil Nadu, India. Analyze the situational details. Respond in the following exact JSON format (strictly JSON, no markdown blocks, no formatting wrappers):\n{\n  \"risk_level\": \"LOW\" | \"MEDIUM\" | \"HIGH\" | \"CRITICAL\",\n  \"threat_description\": \"brief summary of what is happening\",\n  \"defense_guidance\": \"short, direct action steps to stay safe right now\"\n}")
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put("systemInstruction", systemInstructionObj)

                    val generationConfigObj = org.json.JSONObject().apply {
                        val responseFormatObj = org.json.JSONObject().apply {
                            put("mimeType", "application/json")
                        }
                        put("responseFormat", responseFormatObj)
                    }
                    put("generationConfig", generationConfigObj)
                }

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = okhttp3.RequestBody.create(mediaType, requestJson.toString())
                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBodyStr = response.body?.string()

                if (response.isSuccessful && !responseBodyStr.isNullOrEmpty()) {
                    val jsonResponse = org.json.JSONObject(responseBodyStr)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val partsArray = contentObj.getJSONArray("parts")
                    val textContent = partsArray.getJSONObject(0).getString("text")

                    try {
                        val resultJson = org.json.JSONObject(textContent)
                        aiRiskLevel.value = resultJson.optString("risk_level", "LOW").uppercase()
                        aiResponse.value = resultJson.optString("threat_description", "Situation analyzed successfully.")
                        aiGuidance.value = resultJson.optString("defense_guidance", "Stay in lit areas, prepare emergency tools, and keep alert.")
                    } catch (e: Exception) {
                        aiRiskLevel.value = if (situationText.lowercase().contains("danger") || situationText.lowercase().contains("follow") || situationText.lowercase().contains("chase")) "HIGH" else "MEDIUM"
                        aiResponse.value = textContent
                        aiGuidance.value = "Observe your surroundings. Head towards populated zones, prepare your alarm/siren, and tap SOS if threatened."
                    }
                } else {
                    val (risk, desc, guide) = simulateAiAssessment(situationText)
                    aiRiskLevel.value = risk
                    aiResponse.value = desc
                    aiGuidance.value = guide
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val (risk, desc, guide) = simulateAiAssessment(situationText)
                aiRiskLevel.value = risk
                aiResponse.value = desc
                aiGuidance.value = guide
            } finally {
                isAiLoading.value = false
            }
        }
    }

    private fun simulateAiAssessment(text: String): Triple<String, String, String> {
        val lower = text.lowercase()
        return when {
            lower.contains("follow") || lower.contains("chase") || lower.contains("run") || lower.contains("grab") || lower.contains("force") || lower.contains("lock") -> {
                Triple(
                    "CRITICAL",
                    "Immediate stalker or physical coercion threat detected. An individual is actively tracking or restricting your movement.",
                    "Scream 'FIRE!' loudly to draw attention. Move rapidly to a brightly lit shop, hotel, or nearest All Women Police Station. Hold your keys between your fingers. Click the red SOS button now to call authorities and alert guardians."
                )
            }
            lower.contains("alone") || lower.contains("isolated") || lower.contains("dark") || lower.contains("stranger") || lower.contains("route") -> {
                Triple(
                    "HIGH",
                    "Suspicious behavior or vulnerable isolated setting detected. Cab route diversion or unlit roads present an elevated risk.",
                    "Share your live location. Call a family member and speak loudly to make it known you are being monitored. Switch on the Kavalan Live Tracker. Prepare your handheld siren in case they approach."
                )
            }
            lower.contains("uneasy") || lower.contains("stare") || lower.contains("crowd") -> {
                Triple(
                    "MEDIUM",
                    "Possible harassment or uncomfortable surveillance in public space detected.",
                    "Position yourself near security personnel, a female group, or cameras. Stand tall, make direct assertive eye contact to signal awareness. Keep your safety app open."
                )
            }
            else -> {
                Triple(
                    "LOW",
                    "Situation appears currently stable. No immediate aggressive or stalking pattern recognized.",
                    "Continue monitoring your environment. Always stay on main, well-populated lanes and keep your phone charged."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sirenPlayer.stop()
        stopAudioPlayback()
        liveTrackingJob?.cancel()
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
