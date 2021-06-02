package com.dallasgutauckis.vane.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.dallasgutauckis.vane.AppData
import com.dallasgutauckis.vane.Person
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

object AppDataSerializer : Serializer<AppData> {
    override val defaultValue: AppData = AppData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppData {
        try {
            return AppData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: AppData, output: OutputStream) = t.writeTo(output)
}

val Context.personDataStore: DataStore<AppData> by dataStore("app_data.pb", serializer = AppDataSerializer)
