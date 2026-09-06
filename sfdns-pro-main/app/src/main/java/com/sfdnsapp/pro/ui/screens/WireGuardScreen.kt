package com.sfdnsapp.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.sfdnsapp.pro.ui.components.CyberConnectButton
import com.sfdnsapp.pro.ui.theme.CyberCardBorder
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.TextDim
import com.sfdnsapp.pro.ui.theme.TextPrimary
import com.sfdnsapp.pro.ui.theme.TextSecondary
import com.sfdnsapp.pro.wireguard.WireGuardKeys
import com.sfdnsapp.pro.wireguard.WireGuardProfile
import com.sfdnsapp.pro.wireguard.WireGuardViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireGuardScreen(
    viewModel: WireGuardViewModel,
    isPersian: Boolean,
    onToggleConnect: () -> Unit,
    headerContent: @Composable () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val rxBytes by viewModel.rxBytes.collectAsState()
    val txBytes by viewModel.txBytes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            headerContent()

            Spacer(Modifier.height(18.dp))

            CyberConnectButton(
                connectionState = connectionState,
                isPersian = isPersian,
                onClick = onToggleConnect
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WgStatCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPersian) "آپلود" else "Upload",
                    value = formatBytes(txBytes)
                )
                WgStatCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPersian) "دانلود" else "Download",
                    value = formatBytes(rxBytes)
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isPersian) "پروفایل‌های وایرگارد" else "WireGuard profiles",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Row {
                    TextButton(onClick = { showSubscriptionDialog = true }) {
                        Text(if (isPersian) "ایمپورت لینک" else "Import link")
                    }
                    TextButton(onClick = { showManualDialog = true }) {
                        Text(if (isPersian) "افزودن دستی" else "Add manually")
                    }
                }
            }

            if (profiles.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    if (isPersian) "هنوز پروفایلی اضافه نکرده‌اید" else "No profiles yet",
                    color = TextDim
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    profiles.forEach { profile ->
                        WgProfileRow(
                            profile = profile,
                            isActive = profile.id == activeProfileId,
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onDelete = { viewModel.deleteProfile(profile.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showManualDialog) {
        WgManualProfileDialog(
            isPersian = isPersian,
            onDismiss = { showManualDialog = false },
            onSaveText = { text, name ->
                viewModel.importFromConfigText(text, name)
                showManualDialog = false
            },
            onSaveFields = { profile ->
                viewModel.addProfile(profile)
                showManualDialog = false
            }
        )
    }

    if (showSubscriptionDialog) {
        WgSubscriptionDialog(
            isPersian = isPersian,
            viewModel = viewModel,
            onDismiss = {
                showSubscriptionDialog = false
                viewModel.resetImportState()
            }
        )
    }
}

@Composable
private fun WgStatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WgProfileRow(
    profile: WireGuardProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(profile.name, color = TextPrimary)
            Text(
                profile.endpoint ?: "-",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActive) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = TextDim)
            }
        }
    }
}

@Composable
private fun WgManualProfileDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSaveText: (String, String?) -> Unit,
    onSaveFields: (WireGuardProfile) -> Unit
) {
    var mode by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dns by remember { mutableStateOf("1.1.1.1") }
    var publicKey by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var allowedIps by remember { mutableStateOf("0.0.0.0/0, ::/0") }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPersian) "افزودن پروفایل وایرگارد" else "Add WireGuard profile") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text(if (isPersian) "متن کانفیگ" else "Config text") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text(if (isPersian) "فیلد به فیلد" else "Fields") })
                }
                Spacer(Modifier.height(12.dp))
                if (mode == 0) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isPersian) "نام (اختیاری)" else "Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text(if (isPersian) "کانفیگ wg-quick را پیست کنید" else "Paste wg-quick config") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { clipboard.getText()?.text?.let { pastedText = it } }) {
                        Text(if (isPersian) "Paste از کلیپ‌بورد" else "Paste from clipboard")
                    }
                } else {
                    OutlinedTextField(name, { name = it }, label = { Text(if (isPersian) "نام پروفایل" else "Profile name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(privateKey, { privateKey = it }, label = { Text("Private Key") }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { privateKey = WireGuardKeys.generate().privateKeyBase64 }) {
                        Text(if (isPersian) "ساخت کلید جدید" else "Generate new key")
                    }
                    OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(dns, { dns = it }, label = { Text("DNS") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(publicKey, { publicKey = it }, label = { Text("Peer Public Key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint (host:port)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(allowedIps, { allowedIps = it }, label = { Text("Allowed IPs") }, modifier = Modifier.fillMaxWidth())
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (mode == 0) {
                    if (pastedText.isBlank()) {
                        error = if (isPersian) "متن کانفیگ خالی است" else "Config text is empty"
                    } else {
                        onSaveText(pastedText, name.ifBlank { null })
                    }
                } else {
                    if (privateKey.isBlank() || publicKey.isBlank()) {
                        error = if (isPersian) "Private/Public key الزامی است" else "Private/Public key required"
                    } else {
                        onSaveFields(
                            WireGuardProfile(
                                id = UUID.randomUUID().toString(),
                                name = name.ifBlank { "WireGuard" },
                                privateKey = privateKey.trim(),
                                addresses = address.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                dnsServers = dns.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                publicKey = publicKey.trim(),
                                endpoint = endpoint.trim().ifBlank { null },
                                allowedIps = allowedIps.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        )
                    }
                }
            }) { Text(if (isPersian) "ذخیره" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

@Composable
private fun WgSubscriptionDialog(
    isPersian: Boolean,
    viewModel: WireGuardViewModel,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val importState by viewModel.importState.collectAsState()
    val loading = importState is WireGuardViewModel.ImportState.Loading

    LaunchedEffect(importState) {
        if (importState is WireGuardViewModel.ImportState.Success) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPersian) "ایمپورت از لینک ساب‌اسکریپشن" else "Import from subscription link") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (isPersian) "آدرس لینک" else "Subscription URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                (importState as? WireGuardViewModel.ImportState.Error)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !loading, onClick = { viewModel.importFromSubscription(url.trim()) }) {
                Text(if (isPersian) "دریافت" else "Fetch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
