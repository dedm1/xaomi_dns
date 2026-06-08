package com.dedm.dns.widget

import android.content.Context

object StepsCalculator {
    private const val PREFS_NAME = "user_settings"
    private const val KEY_HEIGHT_CM = "height_cm"
    private const val KEY_IS_MALE = "is_male"

    // Коэффициенты для расчёта длины шага
    private const val STEP_COEFFICIENT_MALE = 0.415
    private const val STEP_COEFFICIENT_FEMALE = 0.413

    // Значения по умолчанию
    const val DEFAULT_HEIGHT_CM = 186
    const val DEFAULT_IS_MALE = true

    fun getHeightCm(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HEIGHT_CM, DEFAULT_HEIGHT_CM)
    }

    fun setHeightCm(context: Context, height: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_HEIGHT_CM, height).apply()
    }

    fun isMale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MALE, DEFAULT_IS_MALE)
    }

    fun setIsMale(context: Context, isMale: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_MALE, isMale).apply()
    }

    /**
     * Рассчитывает длину шага в метрах
     * Мужчины: рост * 0.415
     * Женщины: рост * 0.413
     */
    fun getStepLengthMeters(context: Context): Double {
        val heightCm = getHeightCm(context)
        val coefficient = if (isMale(context)) STEP_COEFFICIENT_MALE else STEP_COEFFICIENT_FEMALE
        return (heightCm * coefficient) / 100.0
    }

    /**
     * Рассчитывает расстояние в метрах
     */
    fun calculateDistanceMeters(context: Context, steps: Long): Double {
        return steps * getStepLengthMeters(context)
    }

    /**
     * Форматирует расстояние для отображения
     */
    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()} м"
            else -> "${String.format("%.1f", meters / 1000.0)} км"
        }
    }

    /**
     * Короткий формат для виджета
     */
    fun formatDistanceShort(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()} м"
            else -> {
                val km = meters / 1000.0
                "${String.format("%.1f", km)} км"
            }
        }
    }
}
