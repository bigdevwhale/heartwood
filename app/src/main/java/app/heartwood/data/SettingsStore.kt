package app.heartwood.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** Trees may call (a couple of times a month at most). */
    val calls: Boolean = true,
    /** When the last call went out. */
    val lastCallAt: Long = 0,
    /** Calls already made, as "treeId:key:year", so each happens once a year. */
    val callLog: String = "",
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        calls = this[CALLS] ?: true,
        lastCallAt = this[LAST_CALL] ?: 0,
        callLog = this[CALL_LOG] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[CALLS] = s.calls
            it[LAST_CALL] = s.lastCallAt
            it[CALL_LOG] = s.callLog
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CALLS = booleanPreferencesKey("calls")
        val LAST_CALL = longPreferencesKey("last_call")
        val CALL_LOG = stringPreferencesKey("call_log")
    }
}
