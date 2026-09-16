package com.example.toneprofile

import com.example.network.ApiClient
import com.example.network.dto.ToneProfileDto
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
}
