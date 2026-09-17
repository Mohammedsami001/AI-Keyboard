package com.aikeyboard.app.apprules

import com.aikeyboard.app.network.ApiClient
import com.aikeyboard.app.network.dto.AppRuleDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRulesRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    suspend fun getAppRules(): List<AppRuleDto> {
        return apiClient.api.getAppRules()
    }

    suspend fun updateAppRule(packageName: String, toneOverride: String): AppRuleDto {
        return apiClient.api.updateAppRule(AppRuleDto(appPackageName = packageName, toneOverride = toneOverride))
    }

    suspend fun deleteAppRule(id: String) {
        apiClient.api.deleteAppRule(id)
    }
}
