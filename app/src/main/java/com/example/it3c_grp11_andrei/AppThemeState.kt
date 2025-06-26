package com.example.it3c_grp11_andrei

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object AppThemeState {
    val isDarkMode = mutableStateOf(false)

    private val Context.dataStore by preferencesDataStore(name = "settings")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    fun loadTheme(context: Context) {
        runBlocking {
            val prefs = context.dataStore.data.first()
            isDarkMode.value = prefs[DARK_MODE_KEY] ?: false
        }
    }

    fun saveTheme(context: Context, darkMode: Boolean) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[DARK_MODE_KEY] = darkMode
            }
        }
    }
}
