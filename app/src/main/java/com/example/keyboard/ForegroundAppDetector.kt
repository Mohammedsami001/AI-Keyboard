package com.example.keyboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundAppDetector @Inject constructor() {
    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    fun setCurrentPackage(packageName: String?) {
        _currentPackageName.value = packageName
    }
}
