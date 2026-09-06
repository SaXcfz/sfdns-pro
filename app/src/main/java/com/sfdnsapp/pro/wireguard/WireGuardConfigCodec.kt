package com.sfdnsapp.pro.wireguard

import com.wireguard.config.Config
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Base64

/**
 * پارس/تولید کانفیگ وایرگارد.
 *
 * فرمت‌های ورودی ساب‌اسکریپشن پشتیبانی‌شده (به ترتیب تلاش):
 *  1) یک بلوک wg-quick تکی که با [Interface] شروع می‌شود.
 *  2) چند بلوک wg-quick پشت‌سرهم؛ هرکدام می‌توانند قبلشان یک خط
 *     "# Name: <عنوان>" داشته باشند تا اسم‌گذاری شوند.
 *  3) کل محتوا Base64-شده‌ی یکی از دو حالت بالا.
 *  4) آرایه‌ی JSON: [{ "name": "...", "config": "[Interface]\n..." }, ...]
 *  5) آرایه‌ی JSON با فیلد‌های مسطح (privateKey, address, dns, publicKey,
 *     endpoint, allowedIps, presharedKey, keepalive, mtu).
 */
object WireGuardConfigCodec {

    fun parseSubscriptionContent(rawContent: String): List<WireGuardProfile> {
        val content = rawContent.trim()
        if (content.isEmpty()) return emptyList()

        val candidate = if (looksLikePlainConfigOrJson(content)) content
        else tryBase64Decode(content) ?: content

        val trimmed = candidate.trimStart()
        return when {
            trimmed.startsWith("[") && looksLikeJsonArray(trimmed) -> parseJsonArray(JSONArray(trimmed))
            trimmed.startsWith("{") && looksLikeJsonObject(trimmed) -> parseJsonArray(JSONArray().put(JSONObject(trimmed)))
            else -> parseMultiWgQuickText(candidate)
        }
    }

    private fun looksLikePlainConfigOrJson(s: String): Boolean {
        val t = s.trimStart()
        return t.startsWith("[Interface]") || t.startsWith("#") || t.startsWith("[") || t.startsWith("{")
    }

    private fun looksLikeJsonArray(s: String): Boolean =
        runCatching { JSONTokener(s).nextValue() is JSONArray }.getOrDefault(false)

    private fun looksLikeJsonObject(s: String): Boolean =
        runCatching { JSONTokener(s).nextValue() is JSONObject }.getOrDefault(false)

    private fun tryBase64Decode(s: String): String? = runCatching {
        val cleaned = s.replace("\n", "").replace("\r", "").replace(" ", "")
        String(Base64.getDecoder().decode(cleaned), Charsets.UTF_8)
    }.getOrNull()

    private fun parseMultiWgQuickText(text: String): List<WireGuardProfile> {
        val blocks = mutableListOf<Pair<String?, StringBuilder>>()
        var pendingName: String? = null
        var current: StringBuilder? = null

        text.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            val nameMatch = Regex("""^#\s*(?:Name|name|profile)\s*:\s*(.+)$""").find(line)
            when {
                nameMatch != null -> pendingName = nameMatch.groupValues[1].trim()
                line.trim() == "[Interface]" -> {
                    current = StringBuilder().appendLine(line)
                    blocks.add(pendingName to current!!)
                    pendingName = null
                }
                current != null -> current!!.appendLine(line)
            }
        }

        return blocks.mapNotNull { (name, sb) ->
            runCatching { wgQuickTextToProfile(sb.toString(), name) }.getOrNull()
        }
    }

    private fun parseJsonArray(arr: JSONArray): List<WireGuardProfile> =
        (0 until arr.length()).mapNotNull { index ->
            runCatching {
                val obj = arr.getJSONObject(index)
                val name = if (obj.has("name")) obj.getString("name") else "پروفایل ${index + 1}"
                if (obj.has("config")) {
                    wgQuickTextToProfile(obj.getString("config"), name)
                } else {
                    flatJsonToProfile(obj, name)
                }
            }.getOrNull()
        }

    private fun flatJsonToProfile(obj: JSONObject, name: String): WireGuardProfile {
        fun str(vararg keys: String): String? =
            keys.firstNotNullOfOrNull { k -> obj.optString(k, "").ifBlank { null } }
        fun csv(vararg keys: String): List<String> =
            str(*keys)?.split(",", ";")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        return WireGuardProfile(
            id = WireGuardProfile.newId(),
            name = name,
            privateKey = str("privateKey", "private_key") ?: "",
            addresses = csv("address", "addresses"),
            dnsServers = csv("dns", "dnsServers"),
            mtu = if (obj.has("mtu")) obj.optInt("mtu") else null,
            publicKey = str("publicKey", "public_key") ?: "",
            preSharedKey = str("presharedKey", "preshared_key"),
            endpoint = str("endpoint"),
            allowedIps = csv("allowedIps", "allowed_ips").ifEmpty { listOf("0.0.0.0/0", "::/0") },
            persistentKeepalive = obj.optInt("keepalive", obj.optInt("persistentKeepalive", 25))
        )
    }

    /** یک بلوک تکی wg-quick را با پارسر رسمی کتابخانه اعتبارسنجی و تبدیل می‌کند. */
    fun wgQuickTextToProfile(text: String, nameHint: String? = null): WireGuardProfile {
        val config = Config.parse(java.io.StringReader(text))
        val iface = config.`interface`
        val peer = config.peers.firstOrNull()

        return WireGuardProfile(
            id = WireGuardProfile.newId(),
            name = nameHint?.takeIf { it.isNotBlank() } ?: "پروفایل ایمپورت‌شده",
            privateKey = iface.keyPair.privateKey.toBase64(),
            addresses = iface.addresses.map { it.toString() },
            dnsServers = iface.dnsServers.map { it.hostAddress ?: it.toString() } + iface.dnsSearchDomains,
            mtu = if (iface.mtu.isPresent) iface.mtu.get() else null,
            includedApplications = iface.includedApplications.toList(),
            excludedApplications = iface.excludedApplications.toList(),
            publicKey = peer?.publicKey?.toBase64() ?: "",
            preSharedKey = if (peer?.preSharedKey?.isPresent == true) peer.preSharedKey.get().toBase64() else null,
            endpoint = if (peer?.endpoint?.isPresent == true) peer.endpoint.get().toString() else null,
            allowedIps = peer?.allowedIps?.map { it.toString() } ?: listOf("0.0.0.0/0", "::/0"),
            persistentKeepalive = if (peer?.persistentKeepalive?.isPresent == true) peer.persistentKeepalive.get() else null
        )
    }

    fun profileToWgQuickText(profile: WireGuardProfile): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${profile.privateKey}")
        if (profile.addresses.isNotEmpty()) appendLine("Address = ${profile.addresses.joinToString(", ")}")
        if (profile.dnsServers.isNotEmpty()) appendLine("DNS = ${profile.dnsServers.joinToString(", ")}")
        profile.mtu?.let { appendLine("MTU = $it") }
        if (profile.includedApplications.isNotEmpty())
            appendLine("IncludedApplications = ${profile.includedApplications.joinToString(", ")}")
        if (profile.excludedApplications.isNotEmpty())
            appendLine("ExcludedApplications = ${profile.excludedApplications.joinToString(", ")}")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = ${profile.publicKey}")
        profile.preSharedKey?.let { appendLine("PresharedKey = $it") }
        profile.endpoint?.let { appendLine("Endpoint = $it") }
        if (profile.allowedIps.isNotEmpty()) appendLine("AllowedIPs = ${profile.allowedIps.joinToString(", ")}")
        profile.persistentKeepalive?.let { appendLine("PersistentKeepalive = $it") }
    }

    /** به Config رسمی کتابخانه تبدیل می‌کند (لازم برای اتصال با GoBackend). */
    fun profileToConfig(profile: WireGuardProfile): Config =
        Config.parse(java.io.StringReader(profileToWgQuickText(profile)))
}
