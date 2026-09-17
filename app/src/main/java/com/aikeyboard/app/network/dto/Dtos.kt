package com.aikeyboard.app.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestDto(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String
)

@JsonClass(generateAdapter = true)
data class AuthDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class ToneProfileDto(
    val presetName: String,
    val sampleTexts: String? = null
)

@JsonClass(generateAdapter = true)
data class AppRuleDto(
    val id: String? = null,
    val appPackageName: String,
    val toneOverride: String
)

@JsonClass(generateAdapter = true)
data class RewriteRequest(
    val text: String,
    val tonePreset: String? = null,
    val appContext: String? = null
)

@JsonClass(generateAdapter = true)
data class RewriteResponse(
    val correctedText: String,
    val appliedTone: String,
    val latencyMs: Long
)
