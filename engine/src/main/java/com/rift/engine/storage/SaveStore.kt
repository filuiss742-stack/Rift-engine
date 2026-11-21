package com.rift.engine.storage

import android.content.Context

class SaveStore(context: Context) {
    private val preferences = context.getSharedPreferences("rift_engine_save", Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String =
        preferences.getString(key, defaultValue) ?: defaultValue

    fun putFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float =
        preferences.getFloat(key, defaultValue)
}
