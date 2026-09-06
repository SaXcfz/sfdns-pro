package com.sfdnsapp.pro.wireguard

import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair

object WireGuardKeys {
    data class Generated(val privateKeyBase64: String, val publicKeyBase64: String)

    fun generate(): Generated {
        val pair = KeyPair()
        return Generated(pair.privateKey.toBase64(), pair.publicKey.toBase64())
    }

    fun publicKeyFor(privateKeyBase64: String): String? = runCatching {
        KeyPair(Key.fromBase64(privateKeyBase64)).publicKey.toBase64()
    }.getOrNull()

    fun isValidKey(base64: String): Boolean = runCatching { Key.fromBase64(base64) }.isSuccess
}
