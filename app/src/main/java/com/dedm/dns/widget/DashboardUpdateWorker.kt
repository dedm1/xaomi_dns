package com.dedm.dns.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class DashboardUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var syncSuccessful = false
        try {
            // Читаем шаги асинхронно и сохраняем в кэш
            val steps = StepsReader.readTodaySteps(context)
            if (steps != null) {
                saveStepsToCache(context, steps)
                syncSuccessful = true
            }

            if (syncSuccessful) {
                // Помечаем время успешной синхронизации (сбрасывает индикатор в зелёный)
                FreshnessIndicator.markSynced(context)
            }

            return Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "doWork failed", t)
            return Result.retry()
        } finally {
            // Обновляем все виджеты (это также восстановит корректный цвет индикатора из кэша)
            DashboardWidgetProvider.refreshAllWidgets(context)
        }
    }

    companion object {
        private const val TAG = "DashboardWorker"
        const val WORK_NAME = "dashboard_update_work"

        fun scheduleImmediateUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DashboardUpdateWorker>().build()
            // Немедленное обновление идёт под отдельным именем, не трогает периодическую цепочку
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun saveStepsToCache(context: Context, steps: Long) {
            DashboardWidgetProvider.saveStepsToCache(context, steps)
        }
    }
}
