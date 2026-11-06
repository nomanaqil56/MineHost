package com.minehost.app.features.servers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.ui.components.ConfirmDialog
import com.minehost.app.ui.components.EditionBadge
import com.minehost.app.ui.components.MetricCard
import com.minehost.app.ui.components.ServerStatusBadge
import com.minehost.app.ui.components.TunnelStatusBadge
import com.minehost.app.ui.theme.DarkBackground
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.StatusYellow
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    viewModel: ServerDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateConsole: (String) -> Unit,
    onNavigatePlayers: (String) -> Unit,
    onNavigateBackups: (String) -> Unit,
    onNavigateSettings: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val server = uiState.server

    if (server == null && uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryEmerald)
        }
        return
    }

    if (server == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Server not found", color = TextPrimary)
        }
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = server.name, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Server", tint = StatusRed)
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
                .verticalScroll(rememberScrollState())
        ) {
            // Server Header & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EditionBadge(edition = server.edition)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${server.minecraftVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                ServerStatusBadge(status = uiState.currentStatus)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Start / Stop Button
            Button(
                onClick = { viewModel.toggleServer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRunning) StatusRed else PrimaryEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isRunning) "STOP SERVER" else "START SERVER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Public Endpoint / Connection Card
            ConnectionCard(
                endpoint = uiState.publicEndpoint,
                localPort = server.localPort,
                tunnelStatus = uiState.tunnelStatus,
                claimUrl = uiState.claimUrl,
                context = context
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Players",
                    value = "0 / ${server.maxPlayers}",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Ping",
                    value = uiState.latencyMs?.let { "${it}ms" } ?: "--",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subsystem Navigation Cards
            Text(
                text = "Server Administration",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AdminActionRow(
                title = "Live Terminal Console",
                subtitle = "Stream logs and run server commands",
                icon = Icons.Default.Terminal,
                onClick = { onNavigateConsole(server.id) }
            )

            AdminActionRow(
                title = "Player Management",
                subtitle = "Kick, ban, whitelist, and operator permissions",
                icon = Icons.Default.Security,
                onClick = { onNavigatePlayers(server.id) }
            )

            AdminActionRow(
                title = "Worlds & Backups",
                subtitle = "Export, import, and restore world snapshots",
                icon = Icons.Default.History,
                onClick = { onNavigateBackups(server.id) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDeleteDialog) {
            ConfirmDialog(
                title = "Delete Server?",
                message = "This will permanently remove ${server.name} and all its world data from this device.",
                confirmText = "Delete Permanently",
                isDestructive = true,
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteServer(onNavigateBack)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
fun ConnectionCard(
    endpoint: com.minehost.app.domain.model.PublicEndpoint?,
    localPort: Int,
    tunnelStatus: com.minehost.app.core.common.TunnelStatus,
    claimUrl: String?,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Worldwide Public Address",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                TunnelStatusBadge(status = tunnelStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (claimUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Playit Agent Setup Required",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusYellow,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your tunnel agent must be claimed once to assign your static public domain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(claimUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Claim Agent on Playit.gg")
                        }
                    }
                }
            } else if (endpoint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "IP / Host", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(text = endpoint.hostname, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Port: ${endpoint.port}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }

                        Row {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Server Address", endpoint.fullAddress)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied address to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimary)
                            }

                            IconButton(onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Join my Minecraft server!\nAddress: ${endpoint.hostname}\nPort: ${endpoint.port}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Minecraft Server")
                                context.startActivity(shareIntent)
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Server running locally on port $localPort (LAN only). Enable tunnel or check Diagnostics if Playit agent is not running.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AdminActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
