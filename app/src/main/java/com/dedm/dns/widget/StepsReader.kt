package com.dedm.dns.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object StepsReader {

    private const val TAG = "StepsReader"

    suspend fun readTodaySteps(context: Context): Long? {
        if (!hasPermission(context)) {
            Log.e(TAG, "No ACTIVITY_RECOGNITION permission!")
            return null
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            Log.e(TAG, "SensorManager is null!")
            return null
        }

        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Log.e(TAG, "Step counter sensor not available!")
            return null
        }

        val steps = withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine<Long?> { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val steps = event.values.firstOrNull()?.toLong()
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(steps)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                val registered = sensorManager.registerListener(
                    listener,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
                
                if (registered) {
                    sensorManager.flush(listener)
                } else {
                    Log.e(TAG, "Failed to register listener!")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation {
                    sensorManager.unregisterListener(listener)
                }
            }
        }

        return steps
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
