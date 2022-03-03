package com.egoiapp.egoipushlibrary.handlers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.exceptions.InvalidCategoryException
import com.egoiapp.egoipushlibrary.structures.EgoiConfigs
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONException

class DataStoreHandler(
    private val instance: EgoiPushLibrary
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // [data_store_keys]
    private val preferencesKey = stringPreferencesKey("preferences")
    private val configsKey = stringPreferencesKey("configurations")
    // [end_data_store_keys]

    /**
     * @throws InvalidCategoryException
     */
    suspend fun setDSData(category: String, data: String) {
        if (category !in arrayOf(PREFERENCES, CONFIGS)) {
            throw InvalidCategoryException()
        }

        instance.context.dataStore.edit {
                settings -> settings[getKey(category)] = data
        }
    }

    fun getDSPreferences(): EgoiPreferences {
        var preferences: EgoiPreferences

        runBlocking {
            try {
                preferences = getDSData(PREFERENCES) as EgoiPreferences
            } catch (_: JSONException) {
                preferences = EgoiPreferences()
                setDSData(PREFERENCES, preferences.encode())
            }
        }

        return preferences
    }

    fun getDSConfigs(): EgoiConfigs {
        var configs: EgoiConfigs

        runBlocking {
            try {
                configs = getDSData(CONFIGS) as EgoiConfigs
            } catch (_: JSONException) {
                configs = EgoiConfigs(locationUpdates = false)
                setDSData(CONFIGS, configs.encode())
            }
        }

        return configs
    }

    fun setDSLocationUpdates(status: Boolean) = runBlocking {
        val configs: EgoiConfigs = getDSConfigs()

        if (configs.locationUpdates != status) {
            configs.locationUpdates = status
            setDSData(CONFIGS, configs.encode())
        }
    }

    private fun getKey(category: String): Preferences.Key<String> {
        return if (category === PREFERENCES) preferencesKey else configsKey
    }

    /**
     * @throws InvalidCategoryException
     */
    private suspend fun getDSData(category: String): Any {
        if (category !in arrayOf(PREFERENCES, CONFIGS)) {
            throw InvalidCategoryException()
        }

        val data: String = instance.context.dataStore.data.map { settings ->
            settings[getKey(category)] ?: ""
        }.first()

        return if (category === PREFERENCES)
            EgoiPreferences().decode(data)
        else
            EgoiConfigs().decode(data)
    }

    companion object {
        const val PREFERENCES: String = "preferences"
        const val CONFIGS: String = "configs"
    }
}