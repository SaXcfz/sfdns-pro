package com.sfdnsapp.pro.wireguard

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * ذخیره‌ی پروفایل‌های وایرگارد در همان SharedPreferences اصلی اپ
 * ("sfdns_prefs")، دقیقاً به همون سبکی که DnsViewModel لیست DNSهای
 * اختصاصی رو نگه می‌داره — تا با بقیه‌ی پروژه یکدست بمونه.
 */
class WireGuardRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sfdns_prefs", Context.MODE_PRIVATE)

    fun loadProfiles(): List<WireGuardProfile> {
        val json = prefs.getString(KEY_PROFILES, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { WireGuardProfile.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveProfiles(profiles: List<WireGuardProfile>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    fun loadActiveProfileId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun saveActiveProfileId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    companion object {
        private const val KEY_PROFILES = "wireguard_profiles"
        private const val KEY_ACTIVE_ID = "wireguard_active_profile_id"
    }
}
