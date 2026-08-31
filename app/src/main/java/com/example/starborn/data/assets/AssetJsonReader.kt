package com.example.starborn.data.assets

import android.content.Context
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.core.platform.AssetProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

class AssetJsonReader(
    @PublishedApi internal val assetProvider: AssetProvider,
    @PublishedApi internal val moshi: Moshi
) {

    constructor(context: Context, moshi: Moshi) : this(AndroidAssetProvider(context), moshi)

    fun <T> read(fileName: String, type: Type): T? {
        val json = assetProvider.readText(fileName) ?: return null
        return try {
            moshi.adapter<T>(type).fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T> readObject(fileName: String): T? {
        val adapter: JsonAdapter<T> = moshi.adapter(T::class.java)
        val json = assetProvider.readText(fileName) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T> readList(fileName: String): List<T> {
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter: JsonAdapter<List<T>> = moshi.adapter(listType)
        val json = assetProvider.readText(fileName) ?: return emptyList()
        return try {
            adapter.fromJson(json).orEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    inline fun <reified V> readMap(fileName: String): Map<String, V> {
        val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            V::class.java
        )
        val adapter: JsonAdapter<Map<String, V>> = moshi.adapter(mapType)
        val json = assetProvider.readText(fileName) ?: return emptyMap()
        return try {
            adapter.fromJson(json).orEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun assetExists(path: String): Boolean = assetProvider.exists(path)

    fun list(dir: String): List<String> = assetProvider.list(dir)
}
