package com.dedm.dns.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.ColorInt

object BatteryTemperatureReader {

    fun read(context: Context): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        return if (raw != Int.MIN_VALUE) raw / 10f else 0f
    }

    @ColorInt
    fun getColorForTemp(temp: Float): Int {
        return when {
            temp <= 39f -> COLOR_GREEN
            temp <= 45f -> COLOR_YELLOW
            else -> COLOR_RED
        }
    }

    fun formatTemp(temp: Float): String {
        return "${temp.toInt()}°C"
    }

    private const val COLOR_GREEN = 0xFF4CAF50.toInt()
    private const val COLOR_YELLOW = 0xFFFFEB3B.toInt()
    private const val COLOR_RED = 0xFFF44336.toInt()
}
