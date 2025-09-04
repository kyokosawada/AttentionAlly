package com.example.attentionally.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

/**
 * Handles local session persistence for UX speed and quick role switching.
 *
 * This class acts as a local cache for session data (role, anonymous state) to enable
 * fast UI state and role changes in MVVM architecture. Data here should only be used for UX
 * and local guard purposes, not as a source of authentication truth.
 *
 * Backed by Jetpack DataStore, supporting:
 * - Saving/retrieving current user role
 * - Anonymous/guest session state
 * - Session clearing on logout or user switch
 */
class SessionManager(private val context: Context) {
    companion object {
        private val ROLE_KEY = stringPreferencesKey("user_role")
        private val ANONYMOUS_KEY = booleanPreferencesKey("is_anonymous")
    }

    /**
     * Persists user role (STUDENT/TEACHER) for current session/device.
     *
     * Used for local cache; does not affect remote or backend auth state.
     */
    suspend fun saveUserRole(role: UserRole) {
        context.dataStore.edit { prefs ->
            prefs[ROLE_KEY] = role.name
        }
    }

    /**
     * Returns most recent user role (null if none is cached) for UX/UI/guard purposes.
     *
     * Typically consumed in ViewModel/Repository to enable fast screen/flow selection.
     */
    fun getUserRole(): Flow<UserRole?> {
        return context.dataStore.data.map { prefs ->
            prefs[ROLE_KEY]?.let { UserRole.valueOf(it) }
        }
    }

    /**
     * Clears all locally-stored session/role info (logout, user switch, etc).
     *
     * Resets both role and anonymous state from device-local cache.
     */
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Flow: true if current session is anonymous (guest login, prior to upgrade).
     *
     * Useful for conditionally gating features/UI prior to full account creation.
     */
    fun isAnonymousSession(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[ANONYMOUS_KEY] ?: false
        }
    }

    /**
     * Sets whether session is anonymous; UX only, not cloud-auth source of truth.
     *
     * Should be used as a local-only state for switching flows and UI in the current session.
     */
    suspend fun setAnonymousSession(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ANONYMOUS_KEY] = value
        }
    }
}
