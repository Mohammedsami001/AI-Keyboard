package com.example.toneprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToneProfileViewModel @Inject constructor(
    private val repository: ToneProfileRepository
) : ViewModel() {

    private val _presetName = MutableStateFlow("Casual")
    val presetName: StateFlow<String> = _presetName

    private val _sampleTexts = MutableStateFlow("")
    val sampleTexts: StateFlow<String> = _sampleTexts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getToneProfile()
                _presetName.value = profile.presetName
                _sampleTexts.value = profile.sampleTexts ?: ""
            } catch (e: Exception) {
                // Ignore load error or show message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(preset: String, samples: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            try {
                val profile = repository.updateToneProfile(preset, samples.takeIf { it.isNotBlank() })
                _presetName.value = profile.presetName
                _sampleTexts.value = profile.sampleTexts ?: ""
                _message.value = "Profile updated successfully"
            } catch (e: Exception) {
                _message.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
