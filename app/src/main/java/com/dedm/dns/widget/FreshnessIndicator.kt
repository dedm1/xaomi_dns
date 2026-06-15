package com.dedm.dns.widget

import android.content.Context

object FreshnessIndicator {
    
    private const val PREFS_NAME = "freshness_indicator"
    private const val KEY_LAST_SYNC = "last_sync_time"
    
    fun getColorForCurrentTime(context: Context): Int {
        val lastSync = getLastSyncTime(context)
        val elapsedSeconds = (System.currentTimeMillis() - lastSync) / 1000
        val maxSeconds = 300f // 5 минут
        val fraction = (elapsedSeconds.toFloat() / maxSeconds).coerceIn(0f, 1f)
        
        val green = 0xFF4CAF50.toInt()
        val yellow = 0xFFFF9800.toInt()
        val red = 0xFFF44336.toInt()
        
        return if (fraction < 0.5f) {
            interpolateColor(green, yellow, fraction * 2f)
        } else {
            interpolateColor(yellow, red, (fraction - 0.5f) * 2f)
        }
    }
    
    private fun interpolateColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val startA = (colorStart shr 24) and 0xff
        val startR = (colorStart shr 16) and 0xff
        val startG = (colorStart shr 8) and 0xff
        val startB = colorStart and 0xff

        val endA = (colorEnd shr 24) and 0xff
        val endR = (colorEnd shr 16) and 0xff
        val endG = (colorEnd shr 8) and 0xff
        val endB = colorEnd and 0xff

        val a = (startA + fraction * (endA - startA)).toInt()
        val r = (startR + fraction * (endR - startR)).toInt()
        val g = (startG + fraction * (endG - startG)).toInt()
        val b = (startB + fraction * (endB - startB)).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, System.currentTimeMillis())
    }
    
    fun markSynced(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }
}
