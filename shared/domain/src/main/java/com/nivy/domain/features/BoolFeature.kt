package com.nivy.domain.features

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.nivy.data.datastore.DatastoreKeys
import com.nivy.data.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Immutable
class BoolFeature(
    val key: String,
    val group: FeatureGroup,
    val name: String,
    val description: String,
    private val defaultValue: Boolean
) {
    // The value last read or set. A screen starts from it: starting from the default, a screen
    // drew the other layout for a moment, until the stored value arrived (a compact list opened
    // as cards, then turned compact).
    @Volatile
    private var lastKnown: Boolean? = null

    /** What a screen draws before the stored value arrives. */
    internal val initialValue: Boolean
        get() = lastKnown ?: defaultValue

    @Composable
    fun asEnabledState(): Boolean {
        val context = LocalContext.current
        return remember { enabledFlow(context) }.collectAsState(initialValue).value
    }

    suspend fun isEnabled(appContext: Context): Boolean = enabledFlow(appContext).first()

    fun enabledFlow(appContext: Context): Flow<Boolean> = enabledFlow(appContext.dataStore.data)

    internal fun enabledFlow(preferences: Flow<Preferences>): Flow<Boolean> = preferences.map {
        (it[featureKey] ?: defaultValue).also { enabled -> lastKnown = enabled }
    }

    suspend fun set(appContext: Context, enabled: Boolean) {
        appContext.dataStore.edit {
            it[featureKey] = enabled
        }
        lastKnown = enabled
    }

    private val featureKey: Preferences.Key<Boolean>
        get() = DatastoreKeys.nivyFeature(key)
}
