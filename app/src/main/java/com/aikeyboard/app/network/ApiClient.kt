package com.aikeyboard.app.network

import com.aikeyboard.app.BuildConfig
import com.aikeyboard.app.network.dto.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface AiKeyboardApi {
    @POST("/auth/register")
    suspend fun register(@Body request: AuthRequestDto): AuthDto

    @POST("/auth/login")
    suspend fun login(@Body request: AuthRequestDto): AuthDto

    @POST("/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthDto

    @GET("/users/me/tone-profile")
    suspend fun getToneProfile(): ToneProfileDto

    @PUT("/users/me/tone-profile")
    suspend fun updateToneProfile(@Body request: ToneProfileDto): ToneProfileDto

    @GET("/users/me/app-rules")
    suspend fun getAppRules(): List<AppRuleDto>

    @PUT("/users/me/app-rules")
    suspend fun updateAppRule(@Body request: AppRuleDto): AppRuleDto

    @DELETE("/users/me/app-rules/{id}")
    suspend fun deleteAppRule(@Path("id") id: String)

    @POST("/correction/rewrite")
    suspend fun rewrite(@Body request: RewriteRequest): RewriteResponse

    @POST("/transcription/correct")
    suspend fun correctTranscription(@Body request: RewriteRequest): RewriteResponse
}

class ApiClient(authInterceptor: AuthInterceptor) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: AiKeyboardApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AiKeyboardApi::class.java)
}
