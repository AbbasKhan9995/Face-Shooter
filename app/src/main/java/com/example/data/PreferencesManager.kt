package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("face_shooter_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MAX_UNLOCKED_LEVEL = "max_unlocked_level"
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }

    var maxUnlockedLevel: Int
        get() = prefs.getInt(KEY_MAX_UNLOCKED_LEVEL, 1)
        set(value) {
            val clamped = value.coerceIn(1, 100)
            if (clamped > maxUnlockedLevel) {
                prefs.edit().putInt(KEY_MAX_UNLOCKED_LEVEL, clamped).apply()
            }
        }

    var currentLevel: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) {
            val clamped = value.coerceIn(1, 100)
            prefs.edit().putInt(KEY_CURRENT_LEVEL, clamped).apply()
        }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
        }

    fun unlockNextLevel(completedLevel: Int) {
        val next = (completedLevel + 1).coerceAtMost(100)
        if (next > maxUnlockedLevel) {
            maxUnlockedLevel = next
        }
        currentLevel = next
    }
}
