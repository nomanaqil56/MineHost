package com.minehost.app.features.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minehost.app.core.common.LogLevel
import com.minehost.app.domain.model.ServerLogEntry
import com.minehost.app.ui.theme.ConsoleBackground
import com.minehost.app.ui.theme.ConsoleText
import com.minehost.app.ui.theme.DarkBackground
import com.minehost.app.ui.theme.DarkBorder
import com.minehost.app.ui.theme.DarkSurface
import com.minehost.app.ui.theme.PrimaryEmerald
import com.minehost.app.ui.theme.StatusBlue
import com.minehost.app.ui.theme.StatusRed
import com.minehost.app.ui.theme.StatusYellow
import com.minehost.app.ui.theme.TextPrimary
import com.minehost.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    viewModel: ConsoleViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val commandText by viewModel.commandText.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "Live Terminal Console", color = TextPrimary) },
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
            // Log Stream Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ConsoleBackground)
            ) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    items(logs, key = { it.id }) { logEntry ->
                        LogLineItem(logEntry)
                    }
                }
            }

            // Command Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { viewModel.updateCommand(it) },
                    placeholder = { Text("Enter command (e.g. say, kick, op, save-all)", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendCommand() }),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendCommand() },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryEmerald)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = DarkBackground)
                }
            }
        }
    }
}

@Composable
fun LogLineItem(log: ServerLogEntry) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))

    val color = when (log.level) {
        LogLevel.ERROR -> StatusRed
        LogLevel.WARN -> StatusYellow
        LogLevel.DEBUG -> StatusBlue
        LogLevel.INFO -> ConsoleText
        else -> ConsoleText
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            text = "[$timeStr] ",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = log.message,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
