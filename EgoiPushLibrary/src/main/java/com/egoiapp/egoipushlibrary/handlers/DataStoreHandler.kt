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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DataStoreHandler(
    private val instance: EgoiPushLibrary
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // [data_store_keys]
    private val preferencesKey = stringPreferencesKey("preferences")
    private val configsKey = stringPreferencesKey("configurations")
    // [end_data_store_keys]

    init {
        if (getDSData(CONFIGS) === null) {
            val configs = EgoiConfigs()
            configs.locationUpdates = false

            configs.encode()?.let {
                setDSData(CONFIGS, it)
            }
        }
    }

    fun setDSData(category: String, data: String) {
        val key = getKey(category)

        if (key != null) {
            GlobalScope.launch {
                (this as Context).dataStore.edit { settings ->
                    settings[key] = data
                }
            }
        }
    }

    fun getDSPreferences(): EgoiPreferences? {
        return getDSData(PREFERENCES) as EgoiPreferences?
    }

    fun getDSConfigs(): EgoiConfigs? {
        return getDSData(CONFIGS) as EgoiConfigs?
    }

    fun setDSLocationUpdates(status: Boolean) {
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

    private fun getDSData(category: String): Any? {
        val key = getKey(category)

        if (key != null) {
            val deferred = GlobalScope.async {
                (this as Context).dataStore.data.map { settings ->
                    settings[key] ?: ""
                }.first()
            }

            val data: String?

            runBlocking {
                data = deferred.await()
            }

            if (data != null) {
                return when {
                    category === PREFERENCES -> {
                        EgoiPreferences().decode(data)
                    }
                    category === CONFIGS -> {
                        EgoiConfigs().decode(data)
                    }
                    else -> {
                        null
                    }
                }
            }
        }

        return null
    }

    companion object {
        const val PREFERENCES: String = "preferences"
        const val CONFIGS: String = "configs"
    }
}