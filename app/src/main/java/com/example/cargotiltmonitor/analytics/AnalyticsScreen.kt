package com.example.cargotiltmonitor.analytics

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cargotiltmonitor.actuators.ActuatorConfig
import com.example.cargotiltmonitor.data.HistoryConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AnalyticsEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AnalyticsContent(state = state, onAction = vm::onAction)
}

// Чиста функція: State на вході, жодної логіки — лише малювання.
@Composable
fun AnalyticsContent(state: AnalyticsState, onAction: (AnalyticsAction) -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Аналітика", fontSize = 26.sp)
        Text("Кут нахилу: останні ${state.points.size} записів із БД", fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))

        if (state.points.isEmpty()) {
            Text("Даних поки немає. Запустіть датчик і покрутіть пристрій.")
        } else {
            Row {
                Column(
                    modifier = Modifier.height(260.dp).width(40.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("60°", fontSize = 12.sp)
                    Text("30°", fontSize = 12.sp)
                    Text("0°", fontSize = 12.sp)
                }
                LineChart(state.points, Modifier.weight(1f).height(260.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(timeFormat.format(Date(state.points.first().timestamp)), fontSize = 12.sp)
                Text(timeFormat.format(Date(state.points.last().timestamp)), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Мін: %.1f°   Макс: %.1f°   Середнє: %.1f°".format(state.min, state.max, state.avg), fontSize = 16.sp)
            Text("Остання: %.1f°   Спрацювань актуаторів: ${state.actionCount}".format(state.points.last().value), fontSize = 16.sp)
            Text("Червона пунктирна лінія — поріг ${ActuatorConfig.TILT_THRESHOLD_DEG.toInt()}°, червоні точки — актуатори були активні", fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onAction(AnalyticsAction.Clear) }) { Text("Очистити історію") }
    }
}

// Лінійний графік на Canvas: без сторонніх бібліотек
@Composable
private fun LineChart(points: List<ChartPoint>, modifier: Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dangerColor = Color(0xFFC62828)

    Canvas(modifier) {
        val maxY = HistoryConfig.CHART_Y_MAX
        fun y(v: Float) = size.height * (1f - v.coerceIn(0f, maxY) / maxY)

        for (i in 0..4) {
            val gy = size.height * i / 4f
            drawLine(Color.LightGray, Offset(0f, gy), Offset(size.width, gy), 1f)
        }
        val ty = y(ActuatorConfig.TILT_THRESHOLD_DEG)
        drawLine(
            dangerColor, Offset(0f, ty), Offset(size.width, ty), 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
        )

        if (points.size < 2) return@Canvas
        val dx = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, p ->
            val px = i * dx
            if (i == 0) path.moveTo(px, y(p.value)) else path.lineTo(px, y(p.value))
        }
        drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { i, p ->
            if (p.action) drawCircle(dangerColor, 6f, Offset(i * dx, y(p.value)))
        }
    }
}