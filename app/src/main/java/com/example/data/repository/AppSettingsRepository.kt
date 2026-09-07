package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _musicEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC_ENABLED, true))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _sfxEnabled = MutableStateFlow(prefs.getBoolean(KEY_SFX_ENABLED, true))
    val sfxEnabled: StateFlow<Boolean> = _sfxEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _tutorialCompleted = MutableStateFlow(prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false))
    val tutorialCompleted: StateFlow<Boolean> = _tutorialCompleted.asStateFlow()

    fun setMusicEnabled(enabled: Boolean) {
        _musicEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    fun setSfxEnabled(enabled: Boolean) {
        _sfxEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SFX_ENABLED, enabled).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun setTutorialCompleted(completed: Boolean) {
        _tutorialCompleted.value = completed
        prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "pixelgleam_user_settings"
        private const val KEY_MUSIC_ENABLED = "key_music_enabled"
        private const val KEY_SFX_ENABLED = "key_sfx_enabled"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_TUTORIAL_COMPLETED = "key_tutorial_completed"
    }
}
