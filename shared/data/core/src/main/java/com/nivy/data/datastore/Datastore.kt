package com.nivy.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.preferences.LegacySettingsMigration

typealias NivyDataStore = DataStore<Preferences>

val Context.dataStore: NivyDataStore by preferencesDataStore(
    name = "ivy_wallet_datastore_v1",
    // AppPreferences' values used to live in SharedPreferences and the database; these copy
    // them over once.
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, AppPreferences.LegacySharedPreferencesName),
            LegacySettingsMigration(context),
        )
    }
)
