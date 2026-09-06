package com.sfdnsapp.pro.wireguard

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * لایه‌ی مدیریت تانل روی GoBackend رسمی وایرگارد. کاملاً مستقل از
 * DnsVpnService — پروتکل جدا با موتور جدا (هسته‌ی Go وایرگارد به‌جای
 * VpnService دستی مبتنی بر DNS packet forwarding).
 *
 * چون اندروید فقط یک VpnService فعال هم‌زمان اجازه می‌ده، هماهنگی با
 * DnsVpnService (خاموش کردن یکی قبل از روشن کردن اون یکی) در MainActivity
 * انجام می‌شود، نه اینجا — این کلاس فقط مسئول خودِ تانل وایرگارده.
 */
class WireGuardManager(private val appContext: Context) {

    private val backend: GoBackend by lazy { GoBackend(appContext) }
    private var activeTunnel: SimpleTunnel? = null
    private var statsJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow("disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _rxBytes = MutableStateFlow(0L)
    val rxBytes: StateFlow<Long> = _rxBytes

    private val _txBytes = MutableStateFlow(0L)
    val txBytes: StateFlow<Long> = _txBytes

    val isRunning: Boolean get() = activeTunnel != null && _connectionState.value == "connected"

    fun connect(profile: WireGuardProfile, onError: (String) -> Unit) {
        _connectionState.value = "connecting"
        scope.launch {
            runCatching {
                val config = WireGuardConfigCodec.profileToConfig(profile)
                val tunnel = SimpleTunnel(profile.name.ifBlank { "sfdns-wg" }) { newState ->
                    if (newState == Tunnel.State.DOWN) {
                        _connectionState.value = "disconnected"
                        stopStatsPolling()
                    }
                }
                activeTunnel = tunnel
                backend.setState(tunnel, Tunnel.State.UP, config)
                _connectionState.value = "connected"
                startStatsPolling(tunnel)
            }.onFailure { e ->
                _connectionState.value = "disconnected"
                activeTunnel = null
                onError(e.message ?: "خطای ناشناخته در اتصال وایرگارد")
            }
        }
    }

    fun disconnect() {
        val tunnel = activeTunnel ?: run {
            _connectionState.value = "disconnected"
            return
        }
        scope.launch {
            runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            _connectionState.value = "disconnected"
            stopStatsPolling()
            activeTunnel = null
        }
    }

    private fun startStatsPolling(tunnel: SimpleTunnel) {
        stopStatsPolling()
        statsJob = scope.launch {
            while (true) {
                runCatching {
                    val statistics = backend.getStatistics(tunnel)
                    var rx = 0L
                    var tx = 0L
                    for (peerKey in statistics.peers()) {
                        val peerStats = statistics.peer(peerKey) ?: continue
                        rx += peerStats.rxBytes
                        tx += peerStats.txBytes
                    }
                    _rxBytes.value = rx
                    _txBytes.value = tx
                }
                delay(1500)
            }
        }
    }

    private fun stopStatsPolling() {
        statsJob?.cancel()
        statsJob = null
        _rxBytes.value = 0L
        _txBytes.value = 0L
    }

    private class SimpleTunnel(
        private val tunnelName: String,
        private val onState: (Tunnel.State) -> Unit
    ) : Tunnel {
        override fun getName(): String = tunnelName
        override fun onStateChange(newState: Tunnel.State) = onState(newState)
    }
}
