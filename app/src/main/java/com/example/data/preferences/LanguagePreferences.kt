package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "subahan_fashion_prefs")

class LanguagePreferences(private val context: Context) {
  private val keyIsBangla = booleanPreferencesKey("is_bangla")

  val isBanglaFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[keyIsBangla] ?: true
  }

  suspend fun setBangla(isBangla: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[keyIsBangla] = isBangla
    }
  }
}
