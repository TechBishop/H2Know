package com.example.data.repository

import android.content.Context

/** Remembers the utility a person picked for each ZIP. Stored only on this device. */
object ProviderMemory {
    private const val PREFS = "h2know_provider_choice_v1"

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun recall(zip: String): String? = prefs?.getString(zip, null)?.takeIf { it.isNotBlank() }

    fun remember(zip: String, pwsid: String) {
        if (zip.isBlank() || pwsid.isBlank()) return
        prefs?.edit()?.putString(zip, pwsid)?.apply()
    }
}
