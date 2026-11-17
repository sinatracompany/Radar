package com.signalscope.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private lateinit var model: SignalViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { model.start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[SignalViewModel::class.java]
        setContent {
            SignalScopeScreen(
                model,
                requestWifi = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) },
                requestBluetooth = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissionLauncher.launch(permissions)
                }
            )
        }
    }

    override fun onStart() { super.onStart(); model.start() }
    override fun onStop() { model.stop(); super.onStop() }
}

private val Background = Color(0xFF09111C)
private val Surface = Color(0xFF142335)
private val Line = Color(0xFF23445B)
private val WifiGreen = Color(0xFF4EF0AB)
private val BleBlue = Color(0xFF55B6FF)
private val Gold = Color(0xFFFFD16F)

@Composable
fun SignalScopeScreen(model: SignalViewModel, requestWifi: () -> Unit, requestBluetooth: () -> Unit) {
    val signals by model.signals.collectAsState()
    val status by model.status.collectAsState()
    val wifiStatus by model.wifiStatus.collectAsState()
    val bleStatus by model.bleStatus.collectAsState()
    var tab by remember { mutableStateOf("RADAR") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = signals.firstOrNull { it.id == selectedId }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = WifiGreen, surface = Surface, background = Background,
        onSurface = Color(0xFFE5F2FF), onBackground = Color(0xFFE5F2FF)
    )) {
        Column(Modifier.fillMaxSize().background(Background)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SIGNALSCOPE", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black, color = WifiGreen)
                    Text("LIVE RADIO OBSERVATIONS  •  v0.1.0", style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray)
                }
                Text("${signals.size} SEEN", color = Gold, style = MaterialTheme.typography.labelLarge)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("RADAR", "WI-FI", "BLUETOOTH", "SETTINGS").forEach { item ->
                    Text(
                        item, modifier = Modifier.weight(1f).clickable { tab = item }
                            .padding(vertical = 12.dp),
                        color = if (tab == item) WifiGreen else Color.LightGray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
            HorizontalDivider(color = Line)
            when (tab) {
                "RADAR" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)) {
                    Spacer(Modifier.height(12.dp))
                    StatusCard(status, wifiStatus, bleStatus)
                    Spacer(Modifier.height(12.dp))
                    RadarView(signals, onSelect = { selectedId = it.id })
                    Spacer(Modifier.height(6.dp))
                    Text("Conceptual display — dot angles and rings are NOT measured direction or distance.",
                        color = Gold, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    SignalCounters(signals)
                    Spacer(Modifier.height(10.dp))
                    if (selected != null) ObservationDetails(selected, onWatch = { model.toggleWatchlist(selected.id) })
                    else Text("Tap a dot or a signal in a list to inspect real measurements.",
                        style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                }
                "WI-FI" -> SignalList(signals.filter { it.radio == RadioType.WIFI },
                    "Wi-Fi networks", "Results may be cached or rate-limited by Android.",
                    selectedId, onSelect = { selectedId = it }, onWatch = model::toggleWatchlist,
                    onRefresh = { model.requestWifiScan() })
                "BLUETOOTH" -> SignalList(signals.filter { it.radio == RadioType.BLUETOOTH },
                    "Bluetooth LE", "Addresses can rotate; records might not uniquely identify devices.",
                    selectedId, onSelect = { selectedId = it }, onWatch = model::toggleWatchlist,
                    onRefresh = { model.start() })
                else -> SettingsPanel(model, requestWifi, requestBluetooth, onClear = {
                    model.clearSession(); selectedId = null
                })
            }
        }
    }
}

@Composable
private fun StatusCard(status: String, wifi: String, bluetooth: String) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text(status, fontWeight = FontWeight.Bold, color = WifiGreen)
        Spacer(Modifier.height(5.dp))
        Text("Wi-Fi: $wifi", style = MaterialTheme.typography.bodySmall)
        Text("Bluetooth: $bluetooth", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SignalCounters(signals: List<SignalObservation>) {
    val wifiCount = signals.count { it.radio == RadioType.WIFI }
    val bleCount = signals.count { it.radio == RadioType.BLUETOOTH }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Counter("Wi-Fi", wifiCount.toString(), WifiGreen, Modifier.weight(1f))
        Counter("Bluetooth", bleCount.toString(), BleBlue, Modifier.weight(1f))
        Counter("Watched", signals.count { it.isWatchlisted }.toString(), Gold, Modifier.weight(1f))
    }
}

@Composable
private fun Counter(label: String, number: String, color: Color, modifier: Modifier) {
    Column(modifier.background(Surface, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Text(number, color = color, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun pointFor(signal: SignalObservation, size: Size): Offset {
    // Stable, deterministic layout: hash-derived angle is arbitrary, not a bearing.
    val degrees = (signal.id.hashCode().toLong() and 0xFFFFFFFFL) % 360L
    val angle = degrees * PI / 180.0
    val radius = min(size.width, size.height) * 0.45f *
        (0.22f + (1f - SignalMath.signalQuality(signal.rssi)) * 0.72f)
    return Offset(
        size.width / 2f + cos(angle).toFloat() * radius,
        size.height / 2f + sin(angle).toFloat() * radius
    )
}

@Composable
private fun RadarView(signals: List<SignalObservation>, onSelect: (SignalObservation) -> Unit) {
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val visible = signals.take(80) // Rendering cap; signal list still includes every retained record.
    Canvas(Modifier.fillMaxWidth().height(315.dp)
        .background(Color(0xFF071F28), RoundedCornerShape(12.dp))
        .border(1.dp, Line, RoundedCornerShape(12.dp))
        .onSizeChanged { bounds = it }
        .pointerInput(visible, bounds) {
            detectTapGestures { tap ->
                if (bounds.width == 0 || bounds.height == 0) return@detectTapGestures
                val canvasSize = Size(bounds.width.toFloat(), bounds.height.toFloat())
                val nearest = visible.minByOrNull {
                    val marker = pointFor(it, canvasSize)
                    hypot((tap.x - marker.x).toDouble(), (tap.y - marker.y).toDouble())
                }
                if (nearest != null) {
                    val marker = pointFor(nearest, canvasSize)
                    if (hypot((tap.x - marker.x).toDouble(), (tap.y - marker.y).toDouble()) < 32.0)
                        onSelect(nearest)
                }
            }
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = min(size.width, size.height) * 0.45f
        for (fraction in listOf(.25f, .5f, .75f, 1f)) {
            drawCircle(Line, radius = outer * fraction, center = center, style = Stroke(1.5f))
        }
        drawLine(Line, Offset(center.x - outer, center.y), Offset(center.x + outer, center.y), 1.2f)
        drawLine(Line, Offset(center.x, center.y - outer), Offset(center.x, center.y + outer), 1.2f)
        // Deliberately no rotating "scan sweep"; dots only change when real observations arrive.
        visible.forEach { signal ->
            val point = pointFor(signal, size)
            val dotColor = if (signal.isWatchlisted) Gold
                else if (signal.radio == RadioType.WIFI) WifiGreen else BleBlue
            drawCircle(dotColor.copy(alpha = .16f), radius = 13f, center = point)
            drawCircle(dotColor, radius = 5.5f, center = point)
        }
        drawCircle(WifiGreen, radius = 4f, center = center)
    }
}

@Composable
private fun SignalList(
    signals: List<SignalObservation>, title: String, help: String,
    selectedId: String?, onSelect: (String) -> Unit,
    onWatch: (String) -> Unit, onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        }
        Text(help, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        if (signals.isEmpty()) {
            Text("No observations yet. Check permissions and radio status under Settings.")
        }
        signals.forEach { signal ->
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)
                .background(Surface, RoundedCornerShape(10.dp))
                .clickable { onSelect(signal.id) }.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(signal.label, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${signal.rssi} dBm", color = if (signal.radio == RadioType.WIFI) WifiGreen else BleBlue)
                }
                Text(signal.hardwareId, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                if (selectedId == signal.id) {
                    Spacer(Modifier.height(8.dp))
                    ObservationDetails(signal, onWatch = { onWatch(signal.id) })
                }
            }
        }
    }
}

@Composable
private fun ObservationDetails(signal: SignalObservation, onWatch: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text(signal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${signal.rssi} dBm • ${signal.hardwareId}", color = WifiGreen)
        Text(signal.metadata, style = MaterialTheme.typography.bodySmall)
        Text("Classification: ${signal.classification}", style = MaterialTheme.typography.bodySmall)
        Text("Last observation: ${DateFormat.getDateTimeInstance().format(Date(signal.lastSeen))}",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        RssiChart(signal.samples)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onWatch) {
            Text(if (signal.isWatchlisted) "Remove from watchlist" else "Add to watchlist")
        }
    }
}

@Composable
private fun RssiChart(samples: List<SignalSample>) {
    Column {
        Text("Recorded RSSI history (${samples.size} distinct observations)",
            style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(88.dp).padding(top = 8.dp)) {
            drawLine(Line, Offset(0f, size.height), Offset(size.width, size.height), 1f)
            if (samples.size < 2) return@Canvas
            val minValue = samples.minOf { it.rssi }.toFloat()
            val maxValue = samples.maxOf { it.rssi }.toFloat()
            val spread = (maxValue - minValue).coerceAtLeast(1f)
            val path = Path()
            samples.forEachIndexed { i, sample ->
                val x = size.width * i / (samples.size - 1)
                val y = size.height * (1f - (sample.rssi - minValue) / spread) * .88f + 3f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, WifiGreen, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun SettingsPanel(
    model: SignalViewModel, requestWifi: () -> Unit,
    requestBluetooth: () -> Unit, onClear: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("RADIO CAPABILITIES", style = MaterialTheme.typography.titleLarge, color = WifiGreen)
        Spacer(Modifier.height(10.dp))
        Text("Wi-Fi hardware: ${if (model.hasWifiHardware()) "Available" else "Unavailable"}")
        Text("Bluetooth LE: ${if (model.hasBleHardware()) "Available" else "Unavailable"}")
        Text("Location permission: ${if (model.hasLocationPermission()) "Granted" else "Missing"}")
        Text("Nearby devices permission: ${if (model.hasBlePermissions()) "Granted" else "Missing"}")
        Text("System Location: ${if (model.isLocationEnabled()) "On" else "Off"}")
        Spacer(Modifier.height(14.dp))
        if (!model.hasLocationPermission()) Button(onClick = requestWifi) { Text("Allow Wi-Fi location permission") }
        if (!model.hasBlePermissions()) Button(onClick = requestBluetooth) { Text("Allow Bluetooth scanning") }
        Button(onClick = model::start) { Text("Start / resume scanners") }
        OutlinedButton(onClick = model::stop) { Text("Pause scanning") }
        OutlinedButton(onClick = { model.requestWifiScan() }) { Text("Request fresh Wi-Fi scan") }
        Spacer(Modifier.height(12.dp))
        Text("SESSION DATA", style = MaterialTheme.typography.titleMedium, color = Gold)
        Text("Readings are stored in memory only for this version. Watchlist identifiers are saved locally.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onClear) { Text("Clear session observations") }
        Spacer(Modifier.height(12.dp))
        Text("LIMITATIONS", style = MaterialTheme.typography.titleMedium, color = Gold)
        Text("Wi-Fi scans are throttled by Android. Wi-Fi scan results may be cached. " +
            "Bluetooth addresses may rotate. Radar dots are arbitrary visualization coordinates; " +
            "they do not indicate direction, range, or the location of transmitters. " +
            "No cellular scanner, database history, export, or notification engine is included in v0.1.0.",
            style = MaterialTheme.typography.bodySmall)
    }
}
