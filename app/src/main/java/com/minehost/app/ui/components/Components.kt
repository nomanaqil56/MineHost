package com.minehost.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.core.common.MinecraftEdition
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.DarkSurfaceVariant
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusBlue
import com.minehost.app.ui.theme.StatusGray
import com.minehost.app.ui.theme.StatusGreen
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.StatusYellow
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary

@Composable
fun ServerStatusBadge(status: ServerStatus) {
    val (color, text) = when (status) {
        ServerStatus.ONLINE -> StatusGreen to "ONLINE"
        ServerStatus.STARTING -> StatusYellow to "STARTING..."
        ServerStatus.STOPPING -> StatusYellow to "STOPPING..."
        ServerStatus.STOPPED -> StatusGray to "STOPPED"
        ServerStatus.INSTALLING -> StatusBlue to "INSTALLING..."
        ServerStatus.CRASHED -> StatusRed to "CRASHED"
        ServerStatus.ERROR -> StatusRed to "ERROR"
        ServerStatus.NOT_CREATED -> StatusGray to "NEW"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TunnelStatusBadge(status: TunnelStatus) {
    val (color, text) = when (status) {
        TunnelStatus.CONNECTED -> StatusGreen to "RELAY ONLINE"
        TunnelStatus.CONNECTING -> StatusYellow to "CONNECTING..."
        TunnelStatus.RECONNECTING -> StatusYellow to "RECONNECTING"
        TunnelStatus.DISCONNECTED -> StatusGray to "RELAY OFFLINE"
        TunnelStatus.ERROR -> StatusRed to "RELAY ERROR"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun EditionBadge(edition: MinecraftEdition) {
    val label = when (edition) {
        MinecraftEdition.BEDROCK -> "Bedrock"
        MinecraftEdition.JAVA -> "Java"
        MinecraftEdition.CROSSPLAY -> "Cross-Play"
    }
    Text(
        text = label,
        color = PrimaryEmerald,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PrimaryEmerald.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = TextPrimary) },
        text = { Text(text = message, color = TextSecondary) },
        containerColor = DarkSurface,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) StatusRed else PrimaryEmerald
                )
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = dismissText, color = TextPrimary)
            }
        }
    )
}
