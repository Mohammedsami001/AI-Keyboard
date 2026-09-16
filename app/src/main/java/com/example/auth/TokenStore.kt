package com.example.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[accessTokenKey] != null
    }

    val accessToken: Flow<String?> = dataStore.data.map { it[accessTokenKey] }
    
    suspend fun saveTokens(access: String, refresh: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = access
            preferences[refreshTokenKey] = refresh
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
        }
    }
}
