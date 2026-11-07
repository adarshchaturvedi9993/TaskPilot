package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceRecognitionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeWordRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeWordListening = MutableStateFlow(false)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _wakeWordDetectedEvent = MutableStateFlow<String?>(null)
    val wakeWordDetectedEvent: StateFlow<String?> = _wakeWordDetectedEvent.asStateFlow()

    private var currentWakeWord: String = "Jarvis"
    private var onWakeWordCallback: ((wakeWord: String, followUpCommand: String?) -> Unit)? = null
    private var isWakeWordActive = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start Speak-to-Wake Hotword detection (e.g. listening for "Jarvis", "Tasker", or "Guruji")
     */
    fun startWakeWordListening(
        wakeWord: String,
        onDetected: (wakeWord: String, followUpCommand: String?) -> Unit
    ) {
        if (!isAvailable()) {
            _errorMessage.value = "Speech recognition is not available for wake-word detection."
            return
        }

        currentWakeWord = wakeWord.trim()
        onWakeWordCallback = onDetected
        isWakeWordActive = true
        _isWakeWordListening.value = true

        initWakeWordRecognizer()
    }

    private fun initWakeWordRecognizer() {
        if (!isWakeWordActive) return

        try {
            wakeWordRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
            wakeWordRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isWakeWordListening.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // In wake word mode, errors like NO_MATCH or SPEECH_TIMEOUT are normal idle states.
                        // We schedule a restart to keep listening for the wake word.
                        if (isWakeWordActive) {
                            mainHandler.postDelayed({
                                if (isWakeWordActive) {
                                    startWakeWordIntent()
                                }
                            }, 500)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkWakeWordMatch(matches)
                        } else if (isWakeWordActive) {
                            mainHandler.postDelayed({
                                if (isWakeWordActive) startWakeWordIntent()
                            }, 300)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkWakeWordMatch(matches)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startWakeWordIntent()
        } catch (e: Exception) {
            _isWakeWordListening.value = false
            _errorMessage.value = "Failed to initialize wake word detection: ${e.localizedMessage}"
        }
    }

    private fun startWakeWordIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            wakeWordRecognizer?.startListening(intent)
        } catch (_: Exception) {
            if (isWakeWordActive) {
                mainHandler.postDelayed({ if (isWakeWordActive) startWakeWordIntent() }, 1000)
            }
        }
    }

    private fun checkWakeWordMatch(matches: List<String>) {
        val target = currentWakeWord.lowercase(Locale.ROOT)
        // Also check common phonetic variations of Jarvis, Tasker, Guruji, TaskPilot
        val targetVariants = when (target) {
            "jarvis" -> listOf("jarvis", "jarvis,", "hey jarvis", "ok jarvis")
            "tasker" -> listOf("tasker", "tasker,", "hey tasker", "ok tasker")
            "guruji" -> listOf("guruji", "guru ji", "guru", "hey guruji", "ok guruji")
            "taskpilot" -> listOf("taskpilot", "task pilot", "hey taskpilot")
            else -> listOf(target)
        }

        for (match in matches) {
            val lower = match.lowercase(Locale.ROOT)
            for (variant in targetVariants) {
                if (lower.contains(variant)) {
                    // Hotword detected!
                    triggerHapticFeedback()
                    _wakeWordDetectedEvent.value = currentWakeWord

                    // Extract any command spoken in the same utterance
                    val index = lower.indexOf(variant)
                    val after = match.substring((index + variant.length).coerceAtMost(match.length)).trim()
                    val command = if (after.length > 2) after.replace(Regex("^[\\s,.]+"), "") else null

                    // Pause wake word listener and notify callback
                    stopWakeWordListening()
                    onWakeWordCallback?.invoke(currentWakeWord, command)
                    return
                }
            }
        }

        // No match yet; restart listening loop
        if (isWakeWordActive) {
            mainHandler.postDelayed({
                if (isWakeWordActive) startWakeWordIntent()
            }, 300)
        }
    }

    fun stopWakeWordListening() {
        isWakeWordActive = false
        _isWakeWordListening.value = false
        try {
            wakeWordRecognizer?.stopListening()
            wakeWordRecognizer?.destroy()
        } catch (_: Exception) {}
        wakeWordRecognizer = null
    }

    fun clearWakeWordEvent() {
        _wakeWordDetectedEvent.value = null
    }

    /**
     * Standard voice dictation listening (when microphone button is tapped)
     */
    fun startListening() {
        if (!isAvailable()) {
            _errorMessage.value = "Speech recognition is not supported on this device. Please type your instruction."
            return
        }

        // Temporarily pause wake word if running
        val resumeWakeWord = isWakeWordActive
        stopWakeWordListening()

        stopListening()
        _errorMessage.value = null
        _spokenText.value = ""

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }
                    override fun onError(error: Int) {
                        _isListening.value = false
                        _errorMessage.value = getErrorMessage(error)
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _spokenText.value = matches[0]
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _spokenText.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = "Failed to start voice listener: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Client error occurred."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy."
            SpeechRecognizer.ERROR_SERVER -> "Speech server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Timeout."
            else -> "Speech recognition error ($error)."
        }
    }
}
