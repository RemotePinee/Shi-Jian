package com.shijian.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf

enum class VoiceState {
    IDLE, LISTENING, PROCESSING, ERROR, SUCCESS
}

class VoiceInputManager(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    
    private val _state = mutableStateOf(VoiceState.IDLE)
    val state: State<VoiceState> = _state
    
    private val _text = mutableStateOf("")
    val text: State<String> = _text
    
    @Suppress("SpellCheckingInspection")
    private val _rmsdB = mutableFloatStateOf(0f)
    @Suppress("SpellCheckingInspection")
    val rmsdB: State<Float> = _rmsdB

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _state.value = VoiceState.LISTENING }
        override fun onBeginningOfSpeech() { /* No-op */ }
        override fun onRmsChanged(rmsdB: Float) { _rmsdB.floatValue = rmsdB.coerceIn(0f, 10f) }
        override fun onBufferReceived(buffer: ByteArray?) { /* No-op */ }
        override fun onEndOfSpeech() { _state.value = VoiceState.PROCESSING }
        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "系统网络超时"
                SpeechRecognizer.ERROR_NETWORK -> "系统网络错误"
                SpeechRecognizer.ERROR_AUDIO -> "手机录音错误"
                SpeechRecognizer.ERROR_CLIENT -> "手机引擎错误 (Client)"
                SpeechRecognizer.ERROR_SERVER -> "手机引擎错误 (Server)"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没听清，请再说一遍"
                SpeechRecognizer.ERROR_NO_MATCH -> "未匹配到内容"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音引擎正忙"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "录音权限不足"
                else -> "系统引擎暂不可用 ($error)"
            }
            _text.value = message
            _state.value = VoiceState.ERROR
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _text.value = matches[0]
                _state.value = VoiceState.SUCCESS
            } else {
                _text.value = "未匹配到内容"
                _state.value = VoiceState.ERROR
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) { _text.value = matches[0] }
        }
        override fun onEvent(eventType: Int, params: Bundle?) { /* No-op */ }
    }

    // Removed init { initRecognizer() } to prevent main-thread blocking during screen entry

    private fun initRecognizer() {
        // Try creating regardless of the flag since manifest query is fixed now
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        } catch (_: Exception) {
            recognizer = null
        }
    }

    fun startListening() {
        // Ensure clean state
        try {
            recognizer?.cancel()
        } catch (_: Exception) {}

        if (recognizer == null) initRecognizer()
        
        if (recognizer == null) {
            _text.value = "系统语音服务启动失败"
            _state.value = VoiceState.ERROR
            return
        }

        _state.value = VoiceState.LISTENING
        _text.value = ""
        _rmsdB.floatValue = 0f

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Some devices need this for better stability
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            // If start fails, try one last time with fresh init
            initRecognizer()
            try {
                recognizer?.startListening(intent)
            } catch (e2: Exception) {
                _text.value = "系统引擎启动失败: ${e2.message}"
                _state.value = VoiceState.ERROR
            }
        }
    }

    fun stopListening() {
        _state.value = VoiceState.PROCESSING
        recognizer?.stopListening()
    }

    fun reset() {
        _state.value = VoiceState.IDLE
        _text.value = ""
        _rmsdB.floatValue = 0f
        recognizer?.cancel()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
