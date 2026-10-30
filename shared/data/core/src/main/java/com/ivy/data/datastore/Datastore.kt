package com.ivy.data.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.preferences.LegacySettingsMigration

typealias IvyDataStore = DataStore<Preferences>

val Context.dataStore: IvyDataStore by preferencesDataStore(
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

@Composable
@ReadOnlyComposable
fun datastore(): IvyDataStore {
    return LocalContext.current.dataStore
}
