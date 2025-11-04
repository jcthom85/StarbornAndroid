package com.example.starborn.data.assets

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import java.lang.reflect.Type

class AssetJsonReader(
    @PublishedApi internal val context: Context,
    @PublishedApi internal val moshi: Moshi
) {

    fun <T> read(fileName: String, type: Type): T? {
        val json = try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (io: IOException) {
            io.printStackTrace()
            return null
        }
        return moshi.adapter<T>(type).fromJson(json)
    }

    inline fun <reified T> readObject(fileName: String): T? {
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        val json = try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (io: IOException) {
            io.printStackTrace()
            return null
        }
        return adapter.fromJson(json)
    }

    inline fun <reified T> readList(fileName: String): List<T> {
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter: JsonAdapter<List<T>> = moshi.adapter(listType)
        val json = try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (io: IOException) {
            io.printStackTrace()
            return emptyList()
        }
        return adapter.fromJson(json).orEmpty()
    }

    inline fun <reified V> readMap(fileName: String): Map<String, V> {
        val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            V::class.java
        )
        val adapter: JsonAdapter<Map<String, V>> = moshi.adapter(mapType)
        val json = try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (io: IOException) {
            io.printStackTrace()
            return emptyMap()
        }
        return adapter.fromJson(json).orEmpty()
    }

    fun assetExists(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            context.assets.open(path).use { }
            true
        } catch (io: IOException) {
            false
        }
    }
}
