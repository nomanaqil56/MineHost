package com.minehost.app.features.players

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.domain.model.PlayerModel
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
fun PlayersScreen(
    viewModel: PlayersViewModel,
    onNavigateBack: () -> Unit
) {
    val players by viewModel.players.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "Player Management", color = TextPrimary) },
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
        ) {
            if (players.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No players recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Players who join your server will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(players, key = { it.id }) { player ->
                        PlayerItemCard(
                            player = player,
                            onKick = { viewModel.kickPlayer(player.name) },
                            onToggleOp = { viewModel.toggleOp(player) },
                            onToggleBan = { viewModel.toggleBan(player) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerItemCard(
    player: PlayerModel,
    onKick: () -> Unit,
    onToggleOp: () -> Unit,
    onToggleBan: () -> Unit
) {
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (player.isOp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OP",
                            color = PrimaryEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (player.isBanned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BANNED",
                            color = StatusRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onToggleOp) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Toggle OP",
                        tint = if (player.isOp) PrimaryEmerald else TextSecondary
                    )
                }
                IconButton(onClick = onKick) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Kick Player",
                        tint = StatusYellow
                    )
                }
                IconButton(onClick = onToggleBan) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Ban Player",
                        tint = if (player.isBanned) StatusRed else TextSecondary
                    )
                }
            }
        }
    }
}
