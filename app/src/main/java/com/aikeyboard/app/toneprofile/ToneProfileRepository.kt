package com.aikeyboard.app.toneprofile

import com.aikeyboard.app.network.ApiClient
import com.aikeyboard.app.network.dto.ToneProfileDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToneProfileRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    suspend fun getToneProfile(): ToneProfileDto {
        return apiClient.api.getToneProfile()
    }

    suspend fun updateToneProfile(presetName: String, sampleTexts: String?): ToneProfileDto {
        return apiClient.api.updateToneProfile(ToneProfileDto(presetName, sampleTexts))
    }

    suspend fun clearCache() {
        // TODO: Implement actual local cache clearing if a DB or SharedPreferences is added later.
        // Currently, ToneProfile is fetched directly from the network.
    }
}
