package com.example.stepcounterhunting

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private var initialStepCount = -1
    private var lastNotificationUpdate = 0

    companion object {
        const val CHANNEL_ID = "StepHuntingChannel"
        const val NOTIFICATION_ID = 2001
        const val STEPS_REQUIRED = 100
        const val NOTIFICATION_UPDATE_INTERVAL = 1 // Update every 10 steps

        fun startService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        prefs = getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restore initial step count
        initialStepCount = prefs.getInt("initial_step_count", -1)
        lastNotificationUpdate = prefs.getInt("current_steps", 0)

        // Start as foreground service with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Register sensor listener
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val isHunting = prefs.getBoolean("is_hunting", false)
            if (!isHunting) return

            val huntCompleted = prefs.getBoolean("hunt_completed", false)
            if (huntCompleted) return // Don't process if hunt already completed

            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit()
                    .putInt("initial_step_count", initialStepCount)
                    .putInt("current_steps", 0)
                    .apply()
                lastNotificationUpdate = 0
                return
            }

            val currentSteps = event.values[0].toInt() - initialStepCount

            // Protection against negative values
            val validSteps = if (currentSteps < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit()
                    .putInt("initial_step_count", initialStepCount)
                    .putInt("current_steps", 0)
                    .apply()
                lastNotificationUpdate = 0
                0
            } else {
                currentSteps
            }

            // Cap steps at STEPS_REQUIRED
            val cappedSteps = validSteps.coerceAtMost(STEPS_REQUIRED)

            prefs.edit().putInt("current_steps", cappedSteps).apply()

            // Update notification periodically or when goal reached
            if (cappedSteps >= STEPS_REQUIRED) {
                if (!huntCompleted) {
                    prefs.edit().putBoolean("hunt_completed", true).apply()
                    showCompletionNotification()
                }
            } else if (cappedSteps - lastNotificationUpdate >= NOTIFICATION_UPDATE_INTERVAL) {
                updateProgressNotification(cappedSteps)
                lastNotificationUpdate = cappedSteps
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Hunting Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your hunting progress while counting steps"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val currentSteps = prefs.getInt("current_steps", 0)
        val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
        val isUsingLure = prefs.getBoolean("using_lure", false)
        val progress = (currentSteps * 100) / STEPS_REQUIRED
        val stepsRemaining = (STEPS_REQUIRED - currentSteps).coerceAtLeast(0)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isUsingLure) {
            "🎯 Hunting in $region (Lure Active)"
        } else {
            "🦌 Hunting in $region"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Progress: $currentSteps / $STEPS_REQUIRED steps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS_REQUIRED, currentSteps, false)
            .setSubText("$stepsRemaining steps to go • ${progress}% complete")
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateProgressNotification(steps: Int) {
        val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
        val isUsingLure = prefs.getBoolean("using_lure", false)
        val progress = (steps * 100) / STEPS_REQUIRED
        val stepsRemaining = (STEPS_REQUIRED - steps).coerceAtLeast(0)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isUsingLure) {
            "🎯 Hunting in $region (Lure Active)"
        } else {
            "🦌 Hunting in $region"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Progress: $steps / $STEPS_REQUIRED steps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS_REQUIRED, steps, false)
            .setSubText("$stepsRemaining steps to go • ${progress}% complete")
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val region = prefs.getString("current_region", "Unknown") ?: "Unknown"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 Animal Caught!")
            .setContentText("You've walked $STEPS_REQUIRED steps! Tap to see what you caught!")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setSubText("Hunt complete in $region")
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}