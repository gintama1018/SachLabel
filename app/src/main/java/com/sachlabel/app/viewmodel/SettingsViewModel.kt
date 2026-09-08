package com.sachlabel.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sachlabel.app.data.model.UserLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedLanguage = MutableStateFlow(UserLanguage.ENGLISH)
    val selectedLanguage: StateFlow<UserLanguage> = _selectedLanguage.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    init {
        viewModelScope.launch {
            getApplication<Application>().dataStore.data.collect { prefs ->
                val code = prefs[LANGUAGE_KEY] ?: "en"
                val hasLaunched = prefs[FIRST_LAUNCH_KEY] ?: false
                _selectedLanguage.value = UserLanguage.fromCode(code)
                _isFirstLaunch.value = !hasLaunched
            }
        }
    }

    fun selectLanguage(language: UserLanguage) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = language.code
                prefs[FIRST_LAUNCH_KEY] = true
            }
            _selectedLanguage.value = language
            _isFirstLaunch.value = false
        }
    }

    companion object {
        private val FIRST_LAUNCH_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("has_launched")
    }
}
