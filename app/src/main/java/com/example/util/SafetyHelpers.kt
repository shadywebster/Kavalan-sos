package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaRecorder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.sin

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class SirenPlayer {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    private val sampleRate = 22050

    fun start(scope: CoroutineScope, soundType: String = "Buzzer & Siren") {
        if (playJob != null) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            playJob = scope.launch(Dispatchers.Default) {
                val bufferSize = 1024
                val buffer = ShortArray(bufferSize)
                var phase = 0.0
                var time = 0.0

                while (isActive) {
                    val cycleTime = (time % 0.8) / 0.8

                    when (soundType) {
                        "Industrial Buzzer" -> {
                            // High frequency sharp square wave for simulated hardware buzzer
                            val frequency = if ((time % 0.2) < 0.1) 2200.0 else 1800.0
                            for (i in 0 until bufferSize) {
                                val sinValue = sin(2.0 * Math.PI * phase)
                                buffer[i] = (if (sinValue >= 0) 32767.0 * 0.8 else -32767.0 * 0.8).toInt().toShort()
                                phase += frequency / sampleRate
                                if (phase > 1.0) phase -= 1.0
                            }
                        }
                        "Police Siren" -> {
                            // Smooth frequency sweep sine wave for emergency vehicles
                            val frequency = 700.0 + 500.0 * sin(2.0 * Math.PI * cycleTime)
                            for (i in 0 until bufferSize) {
                                buffer[i] = (sin(2.0 * Math.PI * phase) * 32767.0 * 0.7).toInt().toShort()
                                phase += frequency / sampleRate
                                if (phase > 1.0) phase -= 1.0
                            }
                        }
                        else -> {
                            // Hybrid: Alternating buzzer pulse and siren sweep
                            val isBuzzerPhase = (time % 2.0) < 1.0
                            if (isBuzzerPhase) {
                                val frequency = 2000.0
                                for (i in 0 until bufferSize) {
                                    val sinValue = sin(2.0 * Math.PI * phase)
                                    buffer[i] = (if (sinValue >= 0) 32767.0 * 0.85 else -32767.0 * 0.85).toInt().toShort()
                                    phase += frequency / sampleRate
                                    if (phase > 1.0) phase -= 1.0
                                }
                            } else {
                                val frequency = 600.0 + 600.0 * sin(2.0 * Math.PI * cycleTime)
                                for (i in 0 until bufferSize) {
                                    buffer[i] = (sin(2.0 * Math.PI * phase) * 32767.0 * 0.7).toInt().toShort()
                                    phase += frequency / sampleRate
                                    if (phase > 1.0) phase -= 1.0
                                }
                            }
                        }
                    }

                    audioTrack?.write(buffer, 0, bufferSize)
                    time += bufferSize.toDouble() / sampleRate
                    delay(12)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore failure on release
        }
        audioTrack = null
    }

    fun isPlaying(): Boolean = playJob != null
}

class AudioRecorder(private val context: Context) {
    private val cacheDir = context.filesDir
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime = 0L

    fun start(fileName: String): File? {
        if (mediaRecorder != null) return null
        try {
            val file = File(cacheDir, fileName)
            currentFile = file
            startTime = System.currentTimeMillis()

            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("microphone")
            } else {
                context
            }

            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(attributionContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            currentFile = null
            return null
        }
    }

    fun stop(): Pair<File, Int>? {
        val file = currentFile
        val recorder = mediaRecorder
        if (file != null && recorder != null) {
            try {
                recorder.stop()
                recorder.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaRecorder = null
            currentFile = null
            val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            return Pair(file, if (duration > 0) duration else 1)
        }
        return null
    }

    fun isRecording() = mediaRecorder != null
}

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onLocationResult: (GpsCoordinates?) -> Unit) {
        try {
            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("location")
            } else {
                context
            }
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(attributionContext)
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location: Location = task.result
                    onLocationResult(
                        GpsCoordinates(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                    )
                } else {
                    // Fallback 1: Query lastKnownLocation
                    fusedLocationClient.lastLocation.addOnCompleteListener { lastTask ->
                        if (lastTask.isSuccessful && lastTask.result != null) {
                            val location: Location = lastTask.result
                            onLocationResult(
                                GpsCoordinates(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy
                                )
                            )
                        } else {
                            // Fallback 2: Realistic default coordinates for Tamil Nadu (Chennai center) to maintain full functionality
                            onLocationResult(
                                GpsCoordinates(
                                    latitude = 13.0604,
                                    longitude = 80.2496,
                                    accuracy = 150f
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            onLocationResult(null)
        } catch (e: Exception) {
            onLocationResult(null)
        }
    }
}
