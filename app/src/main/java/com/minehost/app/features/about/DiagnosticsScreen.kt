package com.minehost.app.features.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.services.DeviceThermalState
import com.minehost.app.ui.components.ServerStatusBadge
import com.minehost.app.ui.components.TunnelStatusBadge
import com.minehost.app.ui.theme.DarkBackground
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusGreen
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.StatusYellow
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val thermalColor = when (state.thermalState) {
        DeviceThermalState.NONE, DeviceThermalState.LIGHT -> StatusGreen
        DeviceThermalState.MODERATE -> StatusYellow
        else -> StatusRed
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "System Diagnostics & Setup", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hardware & Compatibility Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Device Architecture & Hardware", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DiagRow("CPU ABI", state.deviceArch, isGood = state.is64BitSupported)
                    DiagRow("OS Version", state.androidVersion, isGood = true)
                    DiagRow("System RAM", "${state.availRamMb} MB free / ${state.totalRamMb} MB total", isGood = state.availRamMb > 500)
                    DiagRow("Storage Space", "${state.freeStorageGb} GB internal available", isGood = state.freeStorageGb > 1)
                }
            }

            // Engine & Runtime Component Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Server Engines & Runtimes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    ComponentStatusRow(
                        title = "Bedrock Engine (PocketMine-MP)",
                        installed = state.isPocketMineInstalled,
                        onDownload = { viewModel.downloadPocketMine() },
                        enabled = !state.isDownloading && state.isPhpRuntimeAvailable
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Public connectivity: MineHost's own control plane and relay network are not available in this build. No third-party tunnel provider is required or started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ComponentStatusRow(
                        title = "Java Engine (PaperMC 1.20.4)",
                        installed = state.isPaperInstalled,
                        onDownload = { viewModel.downloadPaper() },
                        enabled = !state.isDownloading && state.isJavaRuntimeAvailable
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    DiagRow(
                        label = "PHP runtime (required for Bedrock)",
                        value = if (state.isPhpRuntimeAvailable) "Found" else "Missing: Bedrock cannot start",
                        isGood = state.isPhpRuntimeAvailable
                    )
                    DiagRow(
                        label = "Java runtime (required for Paper)",
                        value = if (state.isJavaRuntimeAvailable) "Found" else "Missing: Java cannot start",
                        isGood = state.isJavaRuntimeAvailable
                    )

                    state.downloadError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = err, style = MaterialTheme.typography.bodySmall, color = StatusRed)
                    }

                    if (state.isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        state.downloadStatusMessage?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = PrimaryEmerald)
                        }
                        LinearProgressIndicator(
                            progress = { state.downloadProgress ?: 0f },
                            color = PrimaryEmerald,
                            trackColor = DarkBackground,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }

            // Thermal Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Thermostat, contentDescription = null, tint = thermalColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Thermal State: ${state.thermalState}", style = MaterialTheme.typography.titleMedium, color = thermalColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.thermalState >= DeviceThermalState.SEVERE) {
                            "Warning: Device is overheating. MineHost will throttle or auto-save world to prevent hardware damage."
                        } else {
                            "Operating within normal mobile thermal parameters."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Battery Optimization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = StatusYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Background Execution Exemption", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android OEMs (Samsung, Xiaomi, OnePlus, etc.) aggressively kill background processes. Exempt MineHost to prevent the Minecraft server from terminating when the screen is locked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Disable Battery Optimization")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isGood) TextPrimary else StatusYellow
        )
    }
}

@Composable
fun ComponentStatusRow(
    title: String,
    installed: Boolean,
    onDownload: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (installed) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (installed) StatusGreen else StatusYellow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (installed) "Installed" else "Not Installed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (installed) StatusGreen else StatusYellow
                )
            }
        }

        if (!installed) {
            OutlinedButton(
                onClick = onDownload,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Download", fontSize = 12.sp)
            }
        }
    }
}
