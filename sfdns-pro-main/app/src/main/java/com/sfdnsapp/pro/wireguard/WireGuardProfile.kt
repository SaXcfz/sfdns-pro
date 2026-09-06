package com.sfdnsapp.pro.wireguard

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * پروفایل وایرگارد — کاملاً مستقل از [com.sfdnsapp.pro.data.DnsServer].
 * تبدیل نهایی به com.wireguard.config.Config در لحظه‌ی اتصال انجام می‌شود
 * (در WireGuardConfigCodec) نه اینجا، تا این مدل ساده و قابل ذخیره در
 * SharedPreferences (به همون سبک custom_dns_list در DnsViewModel) بمونه.
 */
data class WireGuardProfile(
    val id: String,
    val name: String,

    // [Interface]
    val privateKey: String,
    val addresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val mtu: Int? = null,
    val includedApplications: List<String> = emptyList(),
    val excludedApplications: List<String> = emptyList(),

    // [Peer] (فعلاً یک peer، رایج‌ترین حالت برای کلاینت‌های موبایل)
    val publicKey: String = "",
    val preSharedKey: String? = null,
    val endpoint: String? = null,
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int? = 25
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        fun fromJson(obj: JSONObject): WireGuardProfile = WireGuardProfile(
            id = obj.optString("id", newId()),
            name = obj.optString("name", "پروفایل وایرگارد"),
            privateKey = obj.optString("privateKey", ""),
            addresses = obj.optJSONArray("addresses").toStringList(),
            dnsServers = obj.optJSONArray("dnsServers").toStringList(),
            mtu = if (obj.has("mtu") && !obj.isNull("mtu")) obj.optInt("mtu") else null,
            includedApplications = obj.optJSONArray("includedApplications").toStringList(),
            excludedApplications = obj.optJSONArray("excludedApplications").toStringList(),
            publicKey = obj.optString("publicKey", ""),
            preSharedKey = obj.optString("preSharedKey", "").ifBlank { null },
            endpoint = obj.optString("endpoint", "").ifBlank { null },
            allowedIps = obj.optJSONArray("allowedIps").toStringList()
                .ifEmpty { listOf("0.0.0.0/0", "::/0") },
            persistentKeepalive = if (obj.has("persistentKeepalive") && !obj.isNull("persistentKeepalive"))
                obj.optInt("persistentKeepalive") else 25
        )

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).map { getString(it) }
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("privateKey", privateKey)
        put("addresses", JSONArray(addresses))
        put("dnsServers", JSONArray(dnsServers))
        mtu?.let { put("mtu", it) }
        put("includedApplications", JSONArray(includedApplications))
        put("excludedApplications", JSONArray(excludedApplications))
        put("publicKey", publicKey)
        preSharedKey?.let { put("preSharedKey", it) }
        endpoint?.let { put("endpoint", it) }
        put("allowedIps", JSONArray(allowedIps))
        persistentKeepalive?.let { put("persistentKeepalive", it) }
    }
}
