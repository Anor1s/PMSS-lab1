package com.example.cargotiltmonitor.ble

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cargotiltmonitor.helpers.BleProfile

@Composable
fun BleScreen(vm: BleViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is BleEvent.ShowMessage -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    BleContent(state = state, onAction = vm::onAction)
}

@Composable
fun BleContent(state: BleState, onAction: (BleAction) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) listState.animateScrollToItem(state.log.lastIndex)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(14.dp).background(
                    if (state.isConnected) Color(0xFF2E7D32) else Color(0xFFC62828), CircleShape
                )
            )
            Text(
                if (state.isConnected) "Connected" else "Disconnected",
                style = MaterialTheme.typography.titleMedium
            )
            Text("· ${state.status.name}", style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { onAction(BleAction.ScanClick) },
            enabled = state.canScan,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Пошук пристроїв") }
        if (state.isScanning) LinearProgressIndicator(Modifier.fillMaxWidth())

        Text("Знайдені пристрої", style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.devices, key = { it.address }) { d ->
                Card(
                    Modifier.fillMaxWidth().clickable(enabled = state.canScan) { onAction(BleAction.DeviceClick(d)) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${d.address} · ${d.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BleProfile.COMMANDS.forEach { cmd ->
                Button(
                    onClick = { onAction(BleAction.SendClick(cmd)) },
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f)
                ) { Text(cmd, fontSize = 11.sp, maxLines = 1) }
            }
        }
        OutlinedButton(
            onClick = { onAction(BleAction.DisconnectClick) },
            enabled = state.isConnected,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Відключитись") }

        Text("Термінал", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111111)).padding(8.dp)) {
            LazyColumn(state = listState) {
                items(state.log) { line ->
                    Text(line, color = Color(0xFF7CFC00), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}