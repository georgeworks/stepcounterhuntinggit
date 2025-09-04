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
import androidx.core.app.NotificationCompat
import android.app.PendingIntent

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private var initialStepCount = -1

    companion object {
        const val CHANNEL_ID = "StepHuntingChannel"
        const val NOTIFICATION_ID = 2001
        const val STEPS_REQUIRED = 100  // Match with HuntFragment

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

            val initialStepCount = prefs.getInt("initial_step_count", -1)
            if (initialStepCount < 0) {
                prefs.edit().putInt("initial_step_count", event.values[0].toInt()).apply()
                prefs.edit().putInt("current_steps", 0).apply()  // Also set current_steps to 0
                return
            }

            val currentSteps = event.values[0].toInt() - initialStepCount

            // Protection against negative values
            val validSteps = if (currentSteps < 0) {
                // Reset if we get negative values (can happen after app reinstall)
                prefs.edit()
                    .putInt("initial_step_count", event.values[0].toInt())
                    .putInt("current_steps", 0)
                    .apply()
                0
            } else {
                currentSteps
            }

            // Cap steps at STEPS_REQUIRED to prevent over-counting
            val cappedSteps = validSteps.coerceAtMost(STEPS_REQUIRED)

            prefs.edit().putInt("current_steps", cappedSteps).apply()

            // Update notification
            updateNotification()

            // Stop sensor updates if goal reached
            if (cappedSteps >= STEPS_REQUIRED) {
                val huntCompleted = prefs.getBoolean("hunt_completed", false)
                if (!huntCompleted) {
                    prefs.edit().putBoolean("hunt_completed", true).apply()
                    // Optionally unregister sensor to save battery
                    // sensorManager?.unregisterListener(this)
                    // Note: Don't unregister if you want to continue tracking for stats
                }
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
        val progress = (currentSteps * 100) / STEPS_REQUIRED
        val stepsRemaining = (STEPS_REQUIRED - currentSteps).coerceAtLeast(0)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🦌 Hunting in $region")
            .setContentText("Progress: $currentSteps / $STEPS_REQUIRED steps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS_REQUIRED, currentSteps, false)
            .setSubText("$stepsRemaining steps to go • ${progress}% complete")
            .build()
    }

    private fun updateNotification(steps: Int) {
        val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
        val progress = (steps * 100) / STEPS_REQUIRED
        val stepsRemaining = (STEPS_REQUIRED - steps).coerceAtLeast(0)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🦌 Hunting in $region")
            .setContentText("Progress: $steps / $STEPS_REQUIRED steps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS_REQUIRED, steps, false)
            .setSubText("$stepsRemaining steps to go • ${progress}% complete")
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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 Animal Caught!")
            .setContentText("You've walked $STEPS_REQUIRED steps! Tap to see what you caught!")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    private fun updateNotification() {
        val currentSteps = prefs.getInt("current_steps", 0)
        val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
        val isUsingLure = prefs.getBoolean("using_lure", false)

        // Create the pending intent for opening MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check if hunt is completed
        if (currentSteps >= STEPS_REQUIRED) {
            // Show completion notification
            val completionNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎉 Goal Reached!")
                .setContentText("You've caught an animal! Tap to see your catch!")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Auto dismiss when tapped
                .setOngoing(false) // Not persistent anymore
                .setSubText("Hunt complete in $region")
                .build()

            notificationManager.notify(NOTIFICATION_ID, completionNotification)

            // Mark hunt as completed
            prefs.edit().putBoolean("hunt_completed", true).apply()
        } else {
            // Show progress notification
            val progress = (currentSteps * 100) / STEPS_REQUIRED
            val stepsRemaining = (STEPS_REQUIRED - currentSteps).coerceAtLeast(0)

            val progressTitle = if (isUsingLure) {
                "🎯 Hunting in $region (Lure Active)"
            } else {
                "🦌 Hunting in $region"
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(progressTitle)
                .setContentText("Progress: $currentSteps / $STEPS_REQUIRED steps")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Persistent while hunting
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(STEPS_REQUIRED, currentSteps, false)
                .setSubText("$stepsRemaining steps to go • ${progress}% complete")
                .setOnlyAlertOnce(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}