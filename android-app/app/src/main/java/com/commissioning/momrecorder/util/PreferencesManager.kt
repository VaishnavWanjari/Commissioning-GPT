package com.commissioning.momrecorder.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mom_recorder_prefs", Context.MODE_PRIVATE)

    var claudeApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var defaultMeetingPlatform: String
        get() = prefs.getString(KEY_PLATFORM, "Video Call") ?: "Video Call"
        set(value) = prefs.edit { putString(KEY_PLATFORM, value) }

    var participantNames: String
        get() = prefs.getString(KEY_PARTICIPANTS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PARTICIPANTS, value) }

    var autoGenerateMom: Boolean
        get() = prefs.getBoolean(KEY_AUTO_GENERATE, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_GENERATE, value) }

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, MODEL_HAIKU) ?: MODEL_HAIKU
        set(value) = prefs.edit { putString(KEY_MODEL, value) }

    fun hasApiKey() = claudeApiKey.isNotBlank()

    companion object {
        private const val KEY_API_KEY = "claude_api_key"
        private const val KEY_PLATFORM = "meeting_platform"
        private const val KEY_PARTICIPANTS = "participant_names"
        private const val KEY_AUTO_GENERATE = "auto_generate_mom"
        private const val KEY_MODEL = "selected_model"

        const val MODEL_HAIKU = "claude-haiku-4-5-20251001"
        const val MODEL_SONNET = "claude-sonnet-4-6"
    }
}
