package com.egoi.egoipushlibrary.handlers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.createDataStore
import com.egoi.egoipushlibrary.EgoiPushLibrary
import com.egoi.egoipushlibrary.structures.EgoiConfigs
import com.egoi.egoipushlibrary.structures.EgoiPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DataStoreHandler(
    private val instance: EgoiPushLibrary
) {
    private lateinit var dataStore: DataStore<Preferences>

    // [data_store_keys]
    private val preferencesKey = stringPreferencesKey("preferences")
    private val configsKey = stringPreferencesKey("configurations")
    // [end_data_store_keys]

    init {
        initDataStore()

        if (getDSData(CONFIGS) === null) {
            val configs = EgoiConfigs()
            configs.locationUpdates = false

            configs.encode()?.let {
                setDSData(CONFIGS, it)
            }
        }
    }

    private fun initDataStore() {
        if (!this::dataStore.isInitialized) {
            dataStore = instance.context.createDataStore(
                name = "settings"
            )
        }
    }

    fun setDSData(category: String, data: String) {
        val key = getKey(category)

        if (key != null) {
            GlobalScope.launch {
                dataStore.edit { settings ->
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
                dataStore.data.map { settings ->
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