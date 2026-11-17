package com.signalscope.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SignalViewModel(app: Application) : AndroidViewModel(app) {
    private val context: Context = app.applicationContext
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val preferences = context.getSharedPreferences("signal_scope", Context.MODE_PRIVATE)
    private val _signals = MutableStateFlow<List<SignalObservation>>(emptyList())
    val signals: StateFlow<List<SignalObservation>> = _signals.asStateFlow()

    private val _status = MutableStateFlow("Idle — start a scan")
    val status: StateFlow<String> = _status.asStateFlow()
    private val _wifiStatus = MutableStateFlow("Not started")
    val wifiStatus: StateFlow<String> = _wifiStatus.asStateFlow()
    private val _bleStatus = MutableStateFlow("Not started")
    val bleStatus: StateFlow<String> = _bleStatus.asStateFlow()
    private val watchlist = preferences.getStringSet("watchlist", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val records = LinkedHashMap<String, SignalObservation>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var publishQueued = false

    private fun publishSoon() {
        if (publishQueued) return
        publishQueued = true
        mainHandler.postDelayed({
            publishQueued = false
            _signals.value = records.values.sortedWith(
                compareByDescending<SignalObservation> { it.isWatchlisted }
                    .thenByDescending { it.lastSeen }
            ).toList()
        }, 180L)
    }
    private var wifiReceiverRegistered = false
    private var bluetoothScanning = false
    private var lastWifiRequestAt = 0L
    private var running = false

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) readWifiResults()
        }
    }

    private val bleCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            recordBle(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::recordBle)
        }

        override fun onScanFailed(errorCode: Int) {
            bluetoothScanning = false
            _bleStatus.value = "Bluetooth scan failed ($errorCode)"
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBlePermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return hasLocationPermission()
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    fun hasWifiHardware(): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
    fun hasBleHardware(): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    @SuppressLint("MissingPermission")
    fun start() {
        running = true
        if (hasWifiHardware() && hasLocationPermission()) {
            if (!wifiReceiverRegistered) {
                ContextCompat.registerReceiver(
                    context, wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                    ContextCompat.RECEIVER_EXPORTED
                )
                wifiReceiverRegistered = true
            }
            if (isLocationEnabled()) {
                readWifiResults()
                requestWifiScan(force = false)
            } else _wifiStatus.value = "Enable system Location for Wi-Fi scan results"
        } else _wifiStatus.value = if (!hasWifiHardware()) "Wi-Fi hardware missing" else "Location permission needed"
        if (hasBleHardware() && hasBlePermissions()) startBluetooth()
        else _bleStatus.value = if (!hasBleHardware()) "BLE hardware missing" else "Nearby devices permission needed"
        _status.value = "Monitoring while app is visible"
    }

    fun stop() {
        running = false
        if (wifiReceiverRegistered) {
            try { context.unregisterReceiver(wifiReceiver) } catch (_: IllegalArgumentException) { }
            wifiReceiverRegistered = false
        }
        stopBluetooth()
        _status.value = "Paused — app not in foreground"
    }

    @SuppressLint("MissingPermission")
    fun requestWifiScan(force: Boolean = true) {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            _wifiStatus.value = "Enable Location permission and Location service"
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (force && now - lastWifiRequestAt < 30_000L) {
            _wifiStatus.value = "Wi-Fi refresh limited; try again in ${30 - (now - lastWifiRequestAt) / 1000}s"
            return
        }
        if (!force && now - lastWifiRequestAt < 30_000L) return
        lastWifiRequestAt = now
        try {
            val initiated = wifiManager.startScan()
            _wifiStatus.value = if (initiated) "Wi-Fi scan requested; awaiting Android results"
                else "Android declined scan (possibly throttled); cached results shown"
        } catch (e: Exception) {
            _wifiStatus.value = "Wi-Fi unavailable: ${e.javaClass.simpleName}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun readWifiResults() {
        if (!hasLocationPermission() || !isLocationEnabled()) return
        try {
            val results = wifiManager.scanResults
            val now = System.currentTimeMillis()
            for (item in results) {
                val bssid = item.BSSID ?: continue
                val id = "W:$bssid"
                // ScanResult.timestamp is microseconds since boot; scanResults may be cached.
                val ageMs = ((SystemClock.elapsedRealtimeNanos() / 1000L - item.timestamp) / 1000L)
                    .coerceAtLeast(0L)
                val observedAt = (now - ageMs).coerceAtLeast(0L)
                val name = item.SSID?.takeIf { it.isNotBlank() } ?: "Hidden Wi-Fi network"
                val metadata = "${item.capabilities} • ${item.frequency} MHz • scan age ${ageMs / 1000}s"
                upsert(id, RadioType.WIFI, name, bssid, item.level, item.frequency,
                    metadata, observedAt)
            }
            _wifiStatus.value = "${results.size} system Wi-Fi results (may be cached)"
        } catch (e: SecurityException) {
            _wifiStatus.value = "Wi-Fi permission denied"
        } catch (e: Exception) {
            _wifiStatus.value = "Wi-Fi read failed: ${e.javaClass.simpleName}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetooth() {
        if (bluetoothScanning || !hasBlePermissions()) return
        try {
            val adapter = bluetoothManager.adapter
            if (adapter?.isEnabled != true) {
                _bleStatus.value = "Turn on Bluetooth"
                return
            }
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                _bleStatus.value = "Bluetooth LE scanner unavailable"
                return
            }
            scanner.startScan(null, ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build(), bleCallback)
            bluetoothScanning = true
            _bleStatus.value = "Bluetooth LE scanning (balanced mode)"
        } catch (e: SecurityException) {
            _bleStatus.value = "Bluetooth permission denied"
        } catch (e: Exception) {
            _bleStatus.value = "Bluetooth start failed: ${e.javaClass.simpleName}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBluetooth() {
        if (!bluetoothScanning) return
        try { bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(bleCallback) }
        catch (_: Exception) { }
        bluetoothScanning = false
        _bleStatus.value = "Paused"
    }

    @SuppressLint("MissingPermission")
    private fun recordBle(result: ScanResult) {
        if (!running || !SignalMath.isReasonableRssi(result.rssi)) return
        try {
            // All record mutations occur on the main thread, avoiding scan-callback races.
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { recordBle(result) }
                return
            }
            val address = result.device.address ?: return
            val displayName = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                ?: "Unnamed BLE device"
            val uuidCount = result.scanRecord?.serviceUuids?.size ?: 0
            val vendorCount = result.scanRecord?.manufacturerSpecificData?.size() ?: 0
            upsert("B:$address", RadioType.BLUETOOTH, displayName, address,
                result.rssi, null, "BLE • $uuidCount service UUIDs • $vendorCount manufacturer fields",
                System.currentTimeMillis())
        } catch (_: SecurityException) { _bleStatus.value = "Bluetooth permission denied" }
    }

    private fun upsert(
        id: String, radio: RadioType, title: String, hardwareId: String,
        rssi: Int, frequencyMhz: Int?, details: String, observedAt: Long
    ) {
        if (!SignalMath.isReasonableRssi(rssi)) return
        val previous = records[id]
        // Android Wi-Fi scans sometimes return the exact same cached reading many times.
        if (previous != null && observedAt <= previous.lastSeen) return
        val samples = (previous?.samples.orEmpty() + SignalSample(observedAt, rssi)).takeLast(80)
        records[id] = SignalObservation(
            id = id, radio = radio, label = title, hardwareId = hardwareId,
            rssi = rssi, frequencyMhz = frequencyMhz, metadata = details,
            firstSeen = previous?.firstSeen ?: observedAt,
            lastSeen = observedAt, samples = samples,
            isWatchlisted = id in watchlist
        )
        // Limit retained observations: prevents growth during long scan sessions.
        if (records.size > 500) {
            val eviction = records.values.filter { !it.isWatchlisted }
                .minByOrNull { it.lastSeen }?.id
            if (eviction != null) records.remove(eviction)
        }
        publishSoon()
    }

    fun toggleWatchlist(id: String) {
        if (!watchlist.add(id)) watchlist.remove(id)
        preferences.edit().putStringSet("watchlist", watchlist.toSet()).apply()
        records[id]?.let { records[id] = it.copy(isWatchlisted = id in watchlist) }
        _signals.value = records.values.sortedByDescending { it.lastSeen }.toList()
    }

    fun clearSession() {
        records.clear()
        _signals.value = emptyList()
        _status.value = "Current session cleared"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
