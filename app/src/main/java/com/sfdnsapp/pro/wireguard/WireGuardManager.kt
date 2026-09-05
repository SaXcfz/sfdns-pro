package com.sfdnsapp.pro.wireguard

import android.content.Context
import android.util.Log
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * Lightweight wrapper around the official WireGuard tunnel library.
 * Keeps DNS and WireGuard completely independent (only one can be UP).
 */
class WireGuardManager(private val context: Context) {

    companion object {
        private const val TAG = "WireGuardManager"
        private const val TUNNEL_NAME = "sfdns-wg"
    }

    private val backend: Backend by lazy { GoBackend(context) }

    private val _state = MutableStateFlow(Tunnel.State.DOWN)
    val state: StateFlow<Tunnel.State> = _state.asStateFlow()

    private val _currentConfigName = MutableStateFlow<String?>(null)
    val currentConfigName: StateFlow<String?> = _currentConfigName.asStateFlow()

    private var activeTunnel: Tunnel? = null
    private var lastConfig: Config? = null

    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME

        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "Tunnel state -> $newState")
            _state.value = newState
        }
    }

    val isRunning: Boolean
        get() = _state.value == Tunnel.State.UP

    /**
     * Parse a standard WireGuard .conf content and bring the tunnel UP.
     * Returns true on success.
     */
    suspend fun connect(configText: String, displayName: String = "WireGuard"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val config = Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))
                lastConfig = config
                _currentConfigName.value = displayName

                backend.setState(tunnel, Tunnel.State.UP, config)
                activeTunnel = tunnel
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect WireGuard", e)
                _state.value = Tunnel.State.DOWN
                false
            }
        }

    /**
     * Bring the tunnel DOWN.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (activeTunnel != null) {
                backend.setState(tunnel, Tunnel.State.DOWN, null)
            }
            activeTunnel = null
            _currentConfigName.value = null
            _state.value = Tunnel.State.DOWN
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect WireGuard", e)
            _state.value = Tunnel.State.DOWN
        }
    }

    /**
     * Quick toggle helper.
     */
    suspend fun toggle(configText: String?, displayName: String = "WireGuard"): Boolean {
        return if (isRunning) {
            disconnect()
            false
        } else {
            if (configText.isNullOrBlank()) {
                Log.w(TAG, "No config provided")
                false
            } else {
                connect(configText, displayName)
            }
        }
    }
}
