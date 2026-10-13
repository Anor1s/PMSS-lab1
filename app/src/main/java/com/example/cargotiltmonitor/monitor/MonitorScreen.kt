package com.example.cargotiltmonitor.monitor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MonitorScreen(vm: MonitorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Слухаємо разові Event
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is MonitorEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    MonitorContent(state = state)
}

// Чиста функція: State на вході, жодної логіки — лише малювання.
@Composable
fun MonitorContent(state: MonitorState) {
    val (statusText, color) = when (state.status) {
        MonitorStatus.NORMAL -> "НОРМА" to Color(0xFF2E7D32)
        MonitorStatus.WARNING -> "ПОПЕРЕДЖЕННЯ" to Color(0xFFF9A825)
        MonitorStatus.DANGER -> "НЕБЕЗПЕКА!" to Color(0xFFC62828)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cargo Tilt Monitor", fontSize = 26.sp)
        Spacer(Modifier.height(24.dp))

        Text("X: %.2f".format(state.x), fontSize = 20.sp)
        Text("Y: %.2f".format(state.y), fontSize = 20.sp)
        Text("Z: %.2f".format(state.z), fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        Text("Кут нахилу: %.1f°".format(state.tilt), fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        // Графічний індикатор (Canvas): шкала 0–60°
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val progress = (state.tilt / 60f).coerceIn(0f, 1f)
            drawLine(
                Color.LightGray, Offset(0f, size.height / 2),
                Offset(size.width, size.height / 2),
                strokeWidth = size.height, cap = StrokeCap.Round
            )
            drawLine(
                color, Offset(0f, size.height / 2),
                Offset(size.width * progress, size.height / 2),
                strokeWidth = size.height, cap = StrokeCap.Round
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(statusText, fontSize = 32.sp, color = color)
    }
}