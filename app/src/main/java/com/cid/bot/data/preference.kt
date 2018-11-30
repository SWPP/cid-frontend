package com.cid.bot.data

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefManager @Inject constructor(private val context: Context) {
    fun getPreference(name: String): SharedPreferences {
        return context.getSharedPreferences(name, 0)
    }

    fun getPreference(id: Int): SharedPreferences {
        return context.getSharedPreferences(context.getString(id), 0)
    }

    fun getKey(id: Int): String {
        return context.getString(id)
    }
}
