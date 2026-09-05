package com.sfdnsapp.pro.data

/**
 * Application connection mode.
 * Only one mode can be active at a time because Android allows only one VpnService.
 */
enum class AppMode {
    DNS,
    WIREGUARD
}
