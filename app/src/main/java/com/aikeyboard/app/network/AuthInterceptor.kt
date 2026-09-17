package com.aikeyboard.app.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aikeyboard.app.network.dto.AuthDto
import com.aikeyboard.app.network.dto.RefreshTokenRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AuthInterceptor(
    private val dataStore: DataStore<Preferences>
) : Interceptor {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val authDtoAdapter = moshi.adapter(AuthDto::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val accessToken = runBlocking {
            dataStore.data.first()[stringPreferencesKey("access_token")]
        }

        val requestWithAuth = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(requestWithAuth)

        if (response.code == 401 && accessToken != null) {
            response.close()
            
            val refreshToken = runBlocking {
                dataStore.data.first()[stringPreferencesKey("refresh_token")]
            }

            if (refreshToken != null) {
                // Synchronous refresh call
                val refreshRequest = originalRequest.newBuilder()
                    .url(originalRequest.url.newBuilder().encodedPath("/auth/refresh").build())
                    .post("{\"refreshToken\":\"$refreshToken\"}".toRequestBody("application/json".toMediaType()))
                    .build()

                val refreshResponse = chain.proceed(refreshRequest)
                if (refreshResponse.isSuccessful) {
                    val bodyString = refreshResponse.body?.string()
                    refreshResponse.close()
                    val newAuthDto = bodyString?.let { authDtoAdapter.fromJson(it) }
                    
                    if (newAuthDto != null) {
                        // Normally we would save this to DataStore, but runBlocking inside interceptor 
                        // editing DataStore might be tricky. For now, the TokenStore will handle updates or we do it here.
                        // We will just do a blocking update here.
                        runBlocking {
                            dataStore.updateData { prefs ->
                                prefs.toMutablePreferences().apply {
                                    set(stringPreferencesKey("access_token"), newAuthDto.accessToken)
                                    set(stringPreferencesKey("refresh_token"), newAuthDto.refreshToken)
                                }
                            }
                        }
                        
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer ${newAuthDto.accessToken}")
                            .build()
                        response = chain.proceed(newRequest)
                    }
                } else {
                    refreshResponse.close()
                    // Refresh failed, clear tokens
                    runBlocking {
                        dataStore.updateData { prefs ->
                            prefs.toMutablePreferences().apply {
                                remove(stringPreferencesKey("access_token"))
                                remove(stringPreferencesKey("refresh_token"))
                            }
                        }
                    }
                }
            }
        }

        return response
    }
}
