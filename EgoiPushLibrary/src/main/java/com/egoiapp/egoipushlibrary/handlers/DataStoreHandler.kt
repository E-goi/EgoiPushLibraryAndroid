package com.egoiapp.egoipushlibrary.handlers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.egoiapp.egoipushlibrary.EgoiPushLibrary
import com.egoiapp.egoipushlibrary.structures.EgoiConfigs
import com.egoiapp.egoipushlibrary.structures.EgoiPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreHandler(
    private val instance: EgoiPushLibrary
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // [data_store_keys]
    private val preferencesKey = stringPreferencesKey("preferences")
    private val configsKey = stringPreferencesKey("configurations")
    // [end_data_store_keys]

    private suspend fun main() {
        if (getDSData(CONFIGS) === null) {
            val configs = EgoiConfigs()
            configs.locationUpdates = false

            configs.encode()?.let {
                setDSData(CONFIGS, it)
            }
        }
    }

    init {
        runBlocking {
            main()
        }
    }

    suspend fun setDSData(category: String, data: String) {
        val key = getKey(category)

        if (key != null) {
            instance.context.dataStore.edit {
                settings -> settings[key] = data
            }
        }
    }

    fun getDSPreferences(): EgoiPreferences? {
        var preferences: EgoiPreferences?

        runBlocking {
            preferences = getDSData(PREFERENCES) as EgoiPreferences?
        }

        return preferences
    }

    fun getDSConfigs(): EgoiConfigs? = runBlocking {
        val deferred = async {
            getDSData(CONFIGS) as EgoiConfigs?
        }

        return@runBlocking deferred.await()
    }

    fun setDSLocationUpdates(status: Boolean) = runBlocking {
        val configs: EgoiConfigs? = getDSConfigs()

        if (configs !== null && configs.locationUpdates != status) {
            configs.locationUpdates = status

            configs.encode()?.let {
                setDSData(category = CONFIGS, data = it)
            }
        }
    }

    private fun getKey(category: String): Preferences.Key<String>? {

        return when {
            category === PREFERENCES -> {
                preferencesKey
            }
            category === CONFIGS -> {
                configsKey
            }
            else -> {
                null
            }
        }
    }

    private suspend fun getDSData(category: String): Any? {
        val key = getKey(category)

        if (key != null) {
            val data: String = instance.context.dataStore.data.map { settings ->
                settings[key] ?: ""
            }.first()

            var dsData: Any? = null

            if (category === PREFERENCES) {
                dsData = EgoiPreferences().decode(data)
            } else if (category === CONFIGS) {
                dsData = EgoiConfigs().decode(data)
            }

            return dsData
        }

        return null
    }

    companion object {
        const val PREFERENCES: String = "preferences"
        const val CONFIGS: String = "configs"
    }
}