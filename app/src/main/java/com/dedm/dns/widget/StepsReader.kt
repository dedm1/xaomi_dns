package com.dedm.dns.widget

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object StepsReader {

    private const val TAG = "StepsReader"

    suspend fun readTodaySteps(context: Context): Long? {
        if (!isHealthConnectAvailable(context)) {
            Log.e(TAG, "Health Connect is not available!")
            return null
        }

        val client = HealthConnectClient.getOrCreate(context)
        
        if (!hasPermissionAsync(context)) {
            Log.e(TAG, "No READ_STEPS permission for Health Connect!")
            return null
        }

        return try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps from Health Connect", e)
            null
        }
    }

    fun isHealthConnectAvailable(context: Context): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissionAsync(context: Context): Boolean {
        if (!isHealthConnectAvailable(context)) {
            return false
        }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return HealthPermission.getReadPermission(StepsRecord::class) in granted
    }

    // Синхронная проверка для виджета (только доступность Health Connect)
    fun hasPermission(context: Context): Boolean {
        return isHealthConnectAvailable(context)
    }
}
