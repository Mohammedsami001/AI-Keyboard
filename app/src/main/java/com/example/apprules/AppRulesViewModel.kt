package com.example.apprules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.dto.AppRuleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppRulesViewModel @Inject constructor(
    private val repository: AppRulesRepository,
    private val appsProvider: InstalledAppsProvider
) : ViewModel() {

    private val _rules = MutableStateFlow<List<AppRuleDto>>(emptyList())
    val rules: StateFlow<List<AppRuleDto>> = _rules

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    init {
        _installedApps.value = appsProvider.getInstalledApps()
        loadRules()
    }

    private fun loadRules() {
        viewModelScope.launch {
            try {
                _rules.value = repository.getAppRules()
            } catch (e: Exception) {
                // error handling
            }
        }
    }

    fun updateRule(packageName: String, tone: String) {
        viewModelScope.launch {
            try {
                val updatedRule = repository.updateAppRule(packageName, tone)
                _rules.value = _rules.value.filter { it.appPackageName != packageName } + updatedRule
            } catch (e: Exception) {
                // error handling
            }
        }
    }

    fun deleteRule(packageName: String) {
        viewModelScope.launch {
            try {
                val rule = _rules.value.find { it.appPackageName == packageName }
                if (rule?.id != null) {
                    repository.deleteAppRule(rule.id)
                    _rules.value = _rules.value.filter { it.id != rule.id }
                }
            } catch (e: Exception) {
                // error handling
            }
        }
    }
}
