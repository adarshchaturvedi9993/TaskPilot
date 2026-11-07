package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("taskpilot_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "DARK") ?: "DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _aiProvider = MutableStateFlow(prefs.getString("ai_provider", "gemini") ?: "gemini")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    // Default to injected BuildConfig.GEMINI_API_KEY if available and not empty, or user saved key
    private val defaultApiKey = try {
        val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
        (field.get(null) as? String) ?: ""
    } catch (_: Exception) {
        ""
    }

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", defaultApiKey) ?: defaultApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _requireSafetyConfirmation = MutableStateFlow(prefs.getBoolean("require_confirmation", true))
    val requireSafetyConfirmation: StateFlow<Boolean> = _requireSafetyConfirmation.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // Speak to Wake ("Jarvis", "Tasker", "Guruji", "TaskPilot")
    private val _speakToWakeEnabled = MutableStateFlow(prefs.getBoolean("speak_to_wake_enabled", false))
    val speakToWakeEnabled: StateFlow<Boolean> = _speakToWakeEnabled.asStateFlow()

    private val _wakeWord = MutableStateFlow(prefs.getString("wake_word", "Jarvis") ?: "Jarvis")
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setAiProvider(provider: String) {
        prefs.edit().putString("ai_provider", provider).apply()
        _aiProvider.value = provider
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _geminiApiKey.value = key
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _selectedModel.value = model
    }

    fun setRequireSafetyConfirmation(enabled: Boolean) {
        prefs.edit().putBoolean("require_confirmation", enabled).apply()
        _requireSafetyConfirmation.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setSpeakToWakeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("speak_to_wake_enabled", enabled).apply()
        _speakToWakeEnabled.value = enabled
    }

    fun setWakeWord(word: String) {
        prefs.edit().putString("wake_word", word).apply()
        _wakeWord.value = word
    }
}
