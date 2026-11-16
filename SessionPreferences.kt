package com.example.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "bubbles_session_prefs")

data class ActiveSession(
    val isLoggedIn: Boolean = false,
    val role: String = "ADMIN", // ADMIN or DRIVER
    val driverId: String? = null,
    val actorName: String = "الإدارة",
    val adminUsername: String = DEFAULT_ADMIN_USERNAME,
    val adminAccessCode: String = DEFAULT_ADMIN_CODE,
    val languageCode: String = "AR"
) {
    companion object {
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_ADMIN_CODE = "605790"
    }
}

class SessionPreferences(private val context: Context) {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val DRIVER_ID = stringPreferencesKey("driver_id")
        val ACTOR_NAME = stringPreferencesKey("actor_name")
        val ADMIN_USERNAME = stringPreferencesKey("admin_username")
        val ADMIN_ACCESS_CODE = stringPreferencesKey("admin_access_code")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    val sessionFlow: Flow<ActiveSession> = context.sessionDataStore.data.map { prefs ->
        ActiveSession(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            role = prefs[Keys.SESSION_ROLE] ?: "ADMIN",
            driverId = prefs[Keys.DRIVER_ID]?.takeIf { it.isNotBlank() },
            actorName = prefs[Keys.ACTOR_NAME] ?: "الإدارة",
            adminUsername = prefs[Keys.ADMIN_USERNAME] ?: ActiveSession.DEFAULT_ADMIN_USERNAME,
            adminAccessCode = prefs[Keys.ADMIN_ACCESS_CODE] ?: ActiveSession.DEFAULT_ADMIN_CODE,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "AR"
        )
    }

    suspend fun loginAsAdmin(actorName: String = "الإدارة") {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.SESSION_ROLE] = "ADMIN"
            prefs[Keys.DRIVER_ID] = ""
            prefs[Keys.ACTOR_NAME] = actorName
        }
    }

    suspend fun loginAsDriver(driverId: String, driverName: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.SESSION_ROLE] = "DRIVER"
            prefs[Keys.DRIVER_ID] = driverId
            prefs[Keys.ACTOR_NAME] = driverName
        }
    }

    suspend fun logout() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs[Keys.DRIVER_ID] = ""
        }
    }

    suspend fun updateAdminCredentials(username: String, accessCode: String) {
        context.sessionDataStore.edit { prefs ->
            val cleanUser = username.trim().lowercase().ifBlank { ActiveSession.DEFAULT_ADMIN_USERNAME }
            val cleanCode = accessCode.trim().ifBlank { ActiveSession.DEFAULT_ADMIN_CODE }
            prefs[Keys.ADMIN_USERNAME] = cleanUser
            prefs[Keys.ADMIN_ACCESS_CODE] = cleanCode
        }
    }

    suspend fun setLanguage(langCode: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = langCode
        }
    }
}
