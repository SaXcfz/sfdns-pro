package com.sfdnsapp.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfdnsapp.pro.data.AppMode

/**
 * Segmented control matching the desired WireGuard / DNS toggle UI.
 */
@Composable
fun ModeToggle(
    currentMode: AppMode,
    isPersian: Boolean,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(52.dp)
            .clip(shape)
            .background(Color(0xFFF0F4F8)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // WireGuard tab
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (currentMode == AppMode.WIREGUARD) Color(0xFF1E293B) else Color.Transparent
                )
                .clickable { onModeSelected(AppMode.WIREGUARD) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WireGuard",
                color = if (currentMode == AppMode.WIREGUARD) Color.White else Color(0xFF1E293B),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // DNS tab
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (currentMode == AppMode.DNS) Color(0xFF2563EB) else Color.Transparent
                )
                .clickable { onModeSelected(AppMode.DNS) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DNS",
                color = if (currentMode == AppMode.DNS) Color.White else Color(0xFF1E293B),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
