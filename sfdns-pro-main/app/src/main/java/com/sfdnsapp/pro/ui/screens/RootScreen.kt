package com.sfdnsapp.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import com.sfdnsapp.pro.wireguard.WireGuardViewModel

enum class VpnProtocol { DNS, WIREGUARD }

/**
 * ریشه‌ی صفحه‌ی اصلی: دو پروتکل کاملاً جدا (DNS و WireGuard)، هرکدام با
 * ViewModel، سرویس و منطق اتصال خودشون، فقط پشت یک سوییچ بالای صفحه
 * مشترک هستند — دقیقاً مثل سگمنت WireGuard/DNS در طرح مرجع.
 *
 * @param onToggleDns همون منطق فعلی toggleVpnConnection برای DNS در MainActivity
 * @param onToggleWireGuard منطق مشابه برای وایرگارد (آماده‌سازی VpnService.prepare + اتصال)
 * @param onSwitchProtocol هر بار کاربر پروتکل رو عوض می‌کنه صدا زده می‌شود؛
 *        MainActivity مسئول قطع کردن پروتکل غیرفعال قبلیه (چون اندروید فقط
 *        یک VpnService فعال هم‌زمان اجازه می‌ده).
 */
@Composable
fun RootScreen(
    dnsViewModel: DnsViewModel,
    wireGuardViewModel: WireGuardViewModel,
    onToggleDns: () -> Unit,
    onToggleWireGuard: () -> Unit,
    onSwitchProtocol: (VpnProtocol) -> Unit
) {
    var protocol by remember { mutableStateOf(VpnProtocol.DNS) }
    val settings by dnsViewModel.settings.collectAsState()
    val isPersian = settings.language == "fa"

    when (protocol) {
        VpnProtocol.DNS -> MainScreen(
            viewModel = dnsViewModel,
            onToggleConnect = onToggleDns,
            protocolSwitch = {
                ProtocolSegmentedSwitch(
                    selected = protocol,
                    isPersian = isPersian,
                    onSelect = {
                        protocol = it
                        onSwitchProtocol(it)
                    }
                )
            }
        )
        VpnProtocol.WIREGUARD -> WireGuardScreen(
            viewModel = wireGuardViewModel,
            isPersian = isPersian,
            onToggleConnect = onToggleWireGuard,
            headerContent = {
                ProtocolSegmentedSwitch(
                    selected = protocol,
                    isPersian = isPersian,
                    onSelect = {
                        protocol = it
                        onSwitchProtocol(it)
                    }
                )
            }
        )
    }
}

@Composable
private fun ProtocolSegmentedSwitch(
    selected: VpnProtocol,
    isPersian: Boolean,
    onSelect: (VpnProtocol) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SegmentItem(
            label = "WireGuard",
            selected = selected == VpnProtocol.WIREGUARD,
            onClick = { onSelect(VpnProtocol.WIREGUARD) }
        )
        SegmentItem(
            label = "DNS",
            selected = selected == VpnProtocol.DNS,
            onClick = { onSelect(VpnProtocol.DNS) }
        )
    }
}

@Composable
private fun SegmentItem(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
