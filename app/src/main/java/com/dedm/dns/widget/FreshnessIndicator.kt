package com.dedm.dns.widget

import android.content.Context

object FreshnessIndicator {
    
    private const val PREFS_NAME = "freshness_indicator"
    private const val KEY_LAST_SYNC = "last_sync_time"
    
    // 60 цветов: зелёный → жёлтый → оранжевый → красный (5 минут = 60 шагов по 5 секунд)
    private val GRADIENT_COLORS = intArrayOf(
        0xFF4CAF50.toInt(), // 00
        0xFF58B74E.toInt(), // 01
        0xFF64BF4C.toInt(), // 02
        0xFF70C74A.toInt(), // 03
        0xFF7CCF48.toInt(), // 04
        0xFF88D746.toInt(), // 05
        0xFF94DF44.toInt(), // 06
        0xFFA0E742.toInt(), // 07
        0xFFACEF40.toInt(), // 08
        0xFFB8F73E.toInt(), // 09
        0xFFC4FF3C.toInt(), // 10
        0xFFCEFC3A.toInt(), // 11
        0xFFD8F938.toInt(), // 12
        0xFFE2F636.toInt(), // 13
        0xFFECF334.toInt(), // 14
        0xFFF6F032.toInt(), // 15
        0xFFF9EE34.toInt(), // 16
        0xFFFBEC36.toInt(), // 17
        0xFFFDEA38.toInt(), // 18
        0xFFFFEB3B.toInt(), // 19
        0xFFFFEB3B.toInt(), // 20
        0xFFFFE234.toInt(), // 21
        0xFFFFD92D.toInt(), // 22
        0xFFFFD026.toInt(), // 23
        0xFFFFC71F.toInt(), // 24
        0xFFFFBE18.toInt(), // 25
        0xFFFFB511.toInt(), // 26
        0xFFFFAC0A.toInt(), // 27
        0xFFFFA303.toInt(), // 28
        0xFFFF9F01.toInt(), // 29
        0xFFFF9B00.toInt(), // 30
        0xFFFF9800.toInt(), // 31
        0xFFFF9300.toInt(), // 32
        0xFFFF8E00.toInt(), // 33
        0xFFFF8900.toInt(), // 34
        0xFFFF8400.toInt(), // 35
        0xFFFF7F00.toInt(), // 36
        0xFFFF7A00.toInt(), // 37
        0xFFFF7500.toInt(), // 38
        0xFFFF7000.toInt(), // 39
        0xFFFF6B00.toInt(), // 40
        0xFFFE6703.toInt(), // 41
        0xFFFD6306.toInt(), // 42
        0xFFFC5F09.toInt(), // 43
        0xFFFB5B0C.toInt(), // 44
        0xFFFA570F.toInt(), // 45
        0xFFF95312.toInt(), // 46
        0xFFF84F15.toInt(), // 47
        0xFFF74B18.toInt(), // 48
        0xFFF6471B.toInt(), // 49
        0xFFF5431E.toInt(), // 50
        0xFFF54021.toInt(), // 51
        0xFFF53D24.toInt(), // 52
        0xFFF53A27.toInt(), // 53
        0xFFF5372A.toInt(), // 54
        0xFFF5342D.toInt(), // 55
        0xFFF53130.toInt(), // 56
        0xFFF52E33.toInt(), // 57
        0xFFF52B35.toInt(), // 58
        0xFFF44336.toInt()  // 59
    )
    
    fun getColorForCurrentTime(context: Context): Int {
        val lastSync = getLastSyncTime(context)
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastSync) / 1000
        
        // Каждые 5 секунд = 1 шаг в градиенте
        // 60 шагов = 300 секунд = 5 минут
        val index = (elapsedSeconds / 5).toInt().coerceIn(0, 59)
        return GRADIENT_COLORS[index]
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
