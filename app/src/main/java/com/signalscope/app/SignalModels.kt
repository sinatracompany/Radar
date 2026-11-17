package com.signalscope.app

enum class RadioType { WIFI, BLUETOOTH }

data class SignalSample(val observedAt: Long, val rssi: Int)

data class SignalObservation(
    val id: String,
    val radio: RadioType,
    val label: String,
    val hardwareId: String,
    val rssi: Int,
    val frequencyMhz: Int?,
    val metadata: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val samples: List<SignalSample>,
    val classification: String = "Unknown",
    val isWatchlisted: Boolean = false
)

// Scanner observations are updated only from actual Android radio APIs.
// Radar positions are conceptual and NEVER represent real transmitter bearing/distance.
object SignalMath {
    fun isReasonableRssi(rssi: Int): Boolean = rssi in -127..0
    fun signalQuality(rssi: Int): Float = ((rssi + 105f) / 75f).coerceIn(0f, 1f)
}
