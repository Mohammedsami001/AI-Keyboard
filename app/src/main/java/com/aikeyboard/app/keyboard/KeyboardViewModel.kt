package com.aikeyboard.app.keyboard

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aikeyboard.app.network.ApiClient
import com.aikeyboard.app.network.dto.RewriteRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class KeyboardViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val foregroundAppDetector: ForegroundAppDetector,
    val voiceInputController: VoiceInputController
) : ViewModel() {

    private var rewriteJob: Job? = null
    private var voiceCorrectionJob: Job? = null

    private val _rewriteState = MutableStateFlow<RewriteState>(RewriteState.Idle)
    val rewriteState: StateFlow<RewriteState> = _rewriteState.asStateFlow()

    private var preRewriteText: String? = null
    
    fun onStartInputView(info: EditorInfo) {
        _rewriteState.value = RewriteState.Idle
    }

    fun onFinishInputView() {
        rewriteJob?.cancel()
        voiceCorrectionJob?.cancel()
        voiceInputController.stopListening()
        _rewriteState.value = RewriteState.Idle
    }

    fun rewrite(inputConnection: InputConnection?) {
        if (inputConnection == null) return
        val currentContextText = getFullText(inputConnection) ?: return
        if (currentContextText.isBlank()) return

        // Snapshot before rewriting
        preRewriteText = currentContextText
        _rewriteState.value = RewriteState.Loading

        rewriteJob?.cancel()
        rewriteJob = viewModelScope.launch {
            try {
                val currentPackage = foregroundAppDetector.getCurrentPackage()
                val response = apiClient.api.rewrite(
                    RewriteRequest(
                        text = currentContextText,
                        appContext = currentPackage
                    )
                )
                
                replaceText(inputConnection, currentContextText.length, response.correctedText)
                _rewriteState.value = RewriteState.Success(response.appliedTone)
            } catch (e: Exception) {
                // If it fails, do not touch original text
                _rewriteState.value = RewriteState.Error("Failed to rewrite text")
            }
        }
    }

    fun undo(inputConnection: InputConnection?) {
        val original = preRewriteText
        if (inputConnection != null && original != null && _rewriteState.value is RewriteState.Success) {
            // Revert back. We need to clear current and set original.
            // A simple delete-then-commit:
            val currentContextText = getFullText(inputConnection)
            if (currentContextText != null) {
                replaceText(inputConnection, currentContextText.length, original)
            }
            _rewriteState.value = RewriteState.Idle
            preRewriteText = null
        }
    }

    fun onVoiceFinalResult(inputConnection: InputConnection?, text: String) {
        if (inputConnection == null || text.isBlank()) return

        voiceCorrectionJob?.cancel()
        voiceCorrectionJob = viewModelScope.launch {
            try {
                val currentPackage = foregroundAppDetector.getCurrentPackage()
                val response = apiClient.api.correctTranscription(
                    RewriteRequest(
                        text = text,
                        appContext = currentPackage
                    )
                )
                inputConnection.commitText(response.correctedText, 1)
            } catch (e: Exception) {
                // On failure, insert uncorrected text? Or error state?
                // The prompt says: "if /transcription/correct fails... show a small inline error state in the keyboard UI and leave the user's original text completely untouched."
                _rewriteState.value = RewriteState.Error("Failed to correct voice input")
            }
        }
    }

    private fun getFullText(inputConnection: InputConnection): String? {
        val before = inputConnection.getTextBeforeCursor(1000, 0) ?: ""
        val after = inputConnection.getTextAfterCursor(1000, 0) ?: ""
        return (before.toString() + after.toString()).takeIf { it.isNotEmpty() }
    }
    
    private fun replaceText(inputConnection: InputConnection, oldLength: Int, newText: String) {
        // Delete current and replace.
        // First select all if we can, or just delete backwards/forwards
        val before = inputConnection.getTextBeforeCursor(1000, 0)?.length ?: 0
        val after = inputConnection.getTextAfterCursor(1000, 0)?.length ?: 0
        inputConnection.deleteSurroundingText(before, after)
        inputConnection.commitText(newText, 1)
    }
}

sealed class RewriteState {
    object Idle : RewriteState()
    object Loading : RewriteState()
    data class Success(val tone: String) : RewriteState()
    data class Error(val message: String) : RewriteState()
}
