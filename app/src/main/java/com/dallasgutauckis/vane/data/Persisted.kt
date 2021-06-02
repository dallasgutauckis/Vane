package com.dallasgutauckis.vane.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

interface Persisted<T> {
    fun get(): T
    fun update(updater: (T) -> T): T
}

class PersistedViaDataStore<T>(val dataStore: DataStore<T>) : Persisted<T> {
    override fun get(): T {
        return runBlocking { dataStore.data.first() }
    }

    override fun update(updater: (T) -> T): T {
        return runBlocking { dataStore.updateData { updater(it) } }
    }
}
