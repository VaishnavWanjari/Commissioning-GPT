package com.commissioning.momrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.commissioning.momrecorder.MainActivity
import com.commissioning.momrecorder.R
import kotlinx.coroutines.*

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 1001
        private const val RESTART_DELAY_MS = 500L

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"

        const val BROADCAST_TRANSCRIPT = "com.commissioning.momrecorder.TRANSCRIPT"
        const val BROADCAST_STATE = "com.commissioning.momrecorder.STATE"
        const val BROADCAST_ERROR = "com.commissioning.momrecorder.ERROR"

        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_IS_FINAL = "extra_is_final"
        const val EXTRA_STATE = "extra_state"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_ERROR_MSG = "extra_error_msg"

        var isRunning = false
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var mainHandler: Handler? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isListening = false
    private var isPaused = false
    private var startTime = 0L
    private var durationJob: Job? = null
    private var consecutiveErrors = 0

    enum class State { IDLE, RECORDING, PAUSED, STOPPED }
    private var state = State.IDLE

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        mainHandler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecordingAndSelf()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("Recording…"),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Recording…"))
        }
        startTime = System.currentTimeMillis()
        state = State.RECORDING
        isPaused = false
        consecutiveErrors = 0
        initSpeechRecognizer()
        startListening()
        startDurationTimer()
        broadcastState(State.RECORDING)
    }

    private fun pauseRecording() {
        isPaused = true
        state = State.PAUSED
        stopListening()
        durationJob?.cancel()
        updateNotification("Paused")
        broadcastState(State.PAUSED)
    }

    private fun resumeRecording() {
        isPaused = false
        state = State.RECORDING
        consecutiveErrors = 0
        startListening()
        startDurationTimer()
        updateNotification("Recording…")
        broadcastState(State.RECORDING)
    }

    private fun stopRecordingAndSelf() {
        state = State.STOPPED
        durationJob?.cancel()
        stopListening()
        destroyRecognizer()
        broadcastState(State.STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initSpeechRecognizer() {
        destroyRecognizer()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastError("Speech recognition not available on this device.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { consecutiveErrors = 0 }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: Bundle?) {
                isListening = false
                consecutiveErrors = 0
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) broadcastTranscript(text, isFinal = true)
                if (!isPaused && state == State.RECORDING) {
                    mainHandler?.postDelayed({ startListening() }, RESTART_DELAY_MS)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) broadcastTranscript(text, isFinal = false)
            }

            override fun onError(error: Int) {
                isListening = false
                consecutiveErrors++
                val msg = speechErrorMessage(error)
                Log.w(TAG, "STT error #$consecutiveErrors: $msg")

                // Actionable errors: notify UI
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    broadcastError("Microphone permission denied. Please grant it in Settings.")
                    stopRecordingAndSelf()
                    return
                }
                if (error == SpeechRecognizer.ERROR_AUDIO) {
                    broadcastError("Cannot access microphone. Is another app using it? Try starting Minutes BEFORE your call.")
                    // Don't stop — keep retrying in case mic frees up
                }

                if (!isPaused && state == State.RECORDING) {
                    val delay = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 300L
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> minOf(2000L * consecutiveErrors, 10000L)
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1500L
                        else -> 800L
                    }
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        mainHandler?.postDelayed({
                            initSpeechRecognizer()
                            startListening()
                        }, delay)
                    } else {
                        mainHandler?.postDelayed({ startListening() }, delay)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (speechRecognizer == null) initSpeechRecognizer()
        if (!isListening && state == State.RECORDING && !isPaused) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }
                isListening = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                isListening = false
                broadcastError("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    private fun stopListening() {
        try {
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "stopListening error", e)
        }
    }

    private fun destroyRecognizer() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            while (isActive && state == State.RECORDING) {
                val elapsed = System.currentTimeMillis() - startTime
                sendBroadcast(Intent(BROADCAST_STATE).apply {
                    putExtra(EXTRA_STATE, state.name)
                    putExtra(EXTRA_DURATION, elapsed)
                })
                delay(1000)
            }
        }
    }

    private fun broadcastTranscript(text: String, isFinal: Boolean) {
        sendBroadcast(Intent(BROADCAST_TRANSCRIPT).apply {
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_IS_FINAL, isFinal)
        })
    }

    private fun broadcastState(s: State) {
        sendBroadcast(Intent(BROADCAST_STATE).apply {
            putExtra(EXTRA_STATE, s.name)
            putExtra(EXTRA_DURATION, System.currentTimeMillis() - startTime)
        })
    }

    private fun broadcastError(message: String) {
        sendBroadcast(Intent(BROADCAST_ERROR).apply {
            putExtra(EXTRA_ERROR_MSG, message)
        })
    }

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Minutes")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(status: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun speechErrorMessage(error: Int) = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
        else -> "Unknown error ($error)"
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        isRunning = false
        durationJob?.cancel()
        serviceScope.cancel()
        stopListening()
        destroyRecognizer()
        super.onDestroy()
    }
}
