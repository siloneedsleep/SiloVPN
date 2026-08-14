package com.example

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "silo_vpn_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        val VPN_CONFIG = stringPreferencesKey("vpn_config")
        val TARGET_APPS = stringPreferencesKey("target_apps")
        val AUTO_VPN_ENABLED = booleanPreferencesKey("auto_vpn_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    val vpnConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VPN_CONFIG] ?: ""
    }

    val targetApps: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_APPS] ?: ""
    }
    
    val autoVpnEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_VPN_ENABLED] ?: false
    }

    suspend fun saveVpnConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[VPN_CONFIG] = config
        }
    }

    suspend fun saveTargetApps(apps: String) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_APPS] = apps
        }
    }
    
    suspend fun setAutoVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_VPN_ENABLED] = enabled
        }
    }
    
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }
}
