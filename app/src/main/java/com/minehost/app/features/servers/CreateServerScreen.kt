package com.minehost.app.features.servers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.core.common.Difficulty
import com.minehost.app.core.common.GameMode
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.ui.theme.DarkBackground
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerScreen(
    viewModel: CreateServerViewModel,
    onNavigateBack: () -> Unit,
    onServerCreated: (String) -> Unit
) {
    val state by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "Create Server — Step ${state.currentStep} of 4", color = TextPrimary) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = StatusRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                when (state.currentStep) {
                    1 -> StepServerIdentity(state, viewModel)
                    2 -> StepMinecraftEdition(state, viewModel)
                    3 -> StepGameplayConfiguration(state, viewModel)
                    4 -> StepReviewAndCreate(state)
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentStep > 1) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        enabled = !state.isCreating
                    ) {
                        Text(text = "Back", color = TextPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (state.currentStep < 4) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Text(text = "Next")
                    }
                } else {
                    Button(
                        onClick = { viewModel.createServer(onServerCreated) },
                        enabled = !state.isCreating,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        if (state.isCreating) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Create Server")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepServerIdentity(state: CreateServerFormState, viewModel: CreateServerViewModel) {
    Column {
        Text(
            text = "Server Identity",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Give your server a name and description.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Server Name") },
            placeholder = { Text("My Survival World") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description / MOTD") },
            placeholder = { Text("Welcome to my server!") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }
}

@Composable
fun StepMinecraftEdition(state: CreateServerFormState, viewModel: CreateServerViewModel) {
    Column {
        Text(
            text = "Select Edition",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Choose which Minecraft clients can join your world.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        EditionOptionCard(
            title = "Bedrock Edition",
            subtitle = "Android, iOS, Windows, Consoles (PocketMine-MP engine, ultra-lightweight)",
            selected = state.edition == MinecraftEdition.BEDROCK,
            onClick = { viewModel.updateEdition(MinecraftEdition.BEDROCK) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        EditionOptionCard(
            title = "Java Edition",
            subtitle = "PC, Mac, Linux (PaperMC engine, plugins, high optimization)",
            selected = state.edition == MinecraftEdition.JAVA,
            onClick = { viewModel.updateEdition(MinecraftEdition.JAVA) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        EditionOptionCard(
            title = "Cross-Play (Bedrock + Java)",
            subtitle = "Not available yet: needs Paper + Geyser, which MineHost cannot run on Android yet",
            selected = false,
            onClick = { /* unsupported: intentionally does nothing */ }
        )
    }
}

@Composable
fun EditionOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PrimaryEmerald else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) PrimaryEmerald else TextPrimary
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = PrimaryEmerald
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StepGameplayConfiguration(state: CreateServerFormState, viewModel: CreateServerViewModel) {
    Column {
        Text(
            text = "Server Configuration",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Configure ports, multiplayer capacity, and game rules.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = state.port.toString(),
            onValueChange = { it.toIntOrNull()?.let { p -> viewModel.updatePort(p) } },
            label = { Text("Server Port") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = state.maxPlayers.toString(),
            onValueChange = { it.toIntOrNull()?.let { m -> viewModel.updateMaxPlayers(m) } },
            label = { Text("Maximum Players") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto tunnel switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Worldwide Public Tunnel", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = "Automatically create a public endpoint so players outside Wi-Fi can join",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Switch(
                checked = state.autoStartTunnel,
                onCheckedChange = { viewModel.updateAutoStartTunnel(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryEmerald)
            )
        }
    }
}

@Composable
fun StepReviewAndCreate(state: CreateServerFormState) {
    Column {
        Text(
            text = "Review Server Settings",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Verify your settings before starting directory preparation.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewRow("Name", state.name)
                ReviewRow("Edition", state.edition.name)
                ReviewRow("Engine", state.engineId)
                ReviewRow("Local Port", state.port.toString())
                ReviewRow("Max Players", state.maxPlayers.toString())
                ReviewRow("Worldwide Tunnel", if (state.autoStartTunnel) "Enabled (Playit.gg)" else "Disabled (LAN only)")
                ReviewRow("Local World Storage", "/files/minehost/servers/")
            }
        }
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

