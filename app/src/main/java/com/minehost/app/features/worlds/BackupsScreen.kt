package com.minehost.app.features.worlds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.domain.model.BackupModel
import com.minehost.app.ui.components.ConfirmDialog
import com.minehost.app.ui.theme.DarkBackground
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupsScreen(
    viewModel: BackupsViewModel,
    onNavigateBack: () -> Unit
) {
    val backups by viewModel.backups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var backupToRestore by remember { mutableStateOf<BackupModel?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupModel?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "World & Snapshots", color = TextPrimary) },
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
        ) {
            // World Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Active World Data", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    val sizeMb = uiState.worldSizeBytes / (1024 * 1024)
                    Text(
                        text = "Current disk size: $sizeMb MB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.createBackup("Manual snapshot") },
                        enabled = !uiState.isCreatingBackup && !uiState.isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Create Backup Snapshot")
                    }

                    if (uiState.isCreatingBackup || uiState.isRestoring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.progress ?: 0f },
                            color = PrimaryEmerald,
                            trackColor = DarkBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saved Snapshots (${backups.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (backups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No backup snapshots found.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(backups, key = { it.id }) { backup ->
                        BackupItemCard(
                            backup = backup,
                            onRestore = { backupToRestore = backup },
                            onDelete = { backupToDelete = backup }
                        )
                    }
                }
            }
        }

        backupToRestore?.let { backup ->
            ConfirmDialog(
                title = "Restore Backup?",
                message = "Restoring snapshot '${backup.id}' will replace the current active world. A safety rollback copy will be preserved automatically.",
                confirmText = "Restore World",
                isDestructive = false,
                onConfirm = {
                    val id = backup.id
                    backupToRestore = null
                    viewModel.restoreBackup(id)
                },
                onDismiss = { backupToRestore = null }
            )
        }

        backupToDelete?.let { backup ->
            ConfirmDialog(
                title = "Delete Backup Snapshot?",
                message = "Are you sure you want to delete '${backup.id}' from disk?",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    val id = backup.id
                    backupToDelete = null
                    viewModel.deleteBackup(id)
                },
                onDismiss = { backupToDelete = null }
            )
        }
    }
}

@Composable
fun BackupItemCard(
    backup: BackupModel,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(backup.createdAt))
    val sizeMb = String.format(Locale.US, "%.1f MB", backup.sizeBytes.toFloat() / (1024 * 1024))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateStr, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = "$sizeMb • SHA: ${backup.checksumSha256.take(8)}...", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                backup.note?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                }
            }

            Row {
                IconButton(onClick = onRestore) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = "Restore", tint = PrimaryEmerald)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed)
                }
            }
        }
    }
}

