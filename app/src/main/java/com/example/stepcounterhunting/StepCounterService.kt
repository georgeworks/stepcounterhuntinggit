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

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private var initialStepCount = -1

    companion object {
        const val CHANNEL_ID = "StepCounterChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_SERVICE = "com.example.stepcounterhunting.STOP_SERVICE"

        fun startService(context: Context, region: String) {
            val intent = Intent(context, StepCounterService::class.java)
            intent.putExtra("region", region)
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

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action from notification
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val region = intent?.getStringExtra("region") ?: prefs.getString("current_region", "Unknown") ?: "Unknown"

        // Restore initial step count
        initialStepCount = prefs.getInt("initial_step_count", -1)

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(region, 0))

        // Register sensor listener
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit().putInt("initial_step_count", initialStepCount).apply()
            }

            val currentSteps = event.values[0].toInt() - initialStepCount

            // Save current steps
            prefs.edit().putInt("current_steps", currentSteps).apply()

            // Update notification with progress
            val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
            updateNotification(region, currentSteps)

            // Check if goal reached
            if (currentSteps >= HuntFragment.STEPS_REQUIRED && !prefs.getBoolean("hunt_completed", false)) {
                // Mark as completed to prevent multiple triggers
                prefs.edit().putBoolean("hunt_completed", true).apply()

                // Update notification to show completion
                showCompletionNotification(region)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter Hunting",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your hunting progress while the app counts your steps"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(region: String, steps: Int): Notification {
        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create stop action for notification
        val stopIntent = Intent(this, StepCounterService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = (steps * 100) / HuntFragment.STEPS_REQUIRED
        val stepsRemaining = (HuntFragment.STEPS_REQUIRED - steps).coerceAtLeast(0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🦌 Hunting in $region")
            .setContentText("Progress: $steps / ${HuntFragment.STEPS_REQUIRED} steps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(HuntFragment.STEPS_REQUIRED, steps, false)
            .setSubText("$stepsRemaining steps to go • ${progress}% complete")
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Hunting",
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You're ${progress}% of the way to catching an animal! Keep walking $stepsRemaining more steps.")
            )
            .build()
    }

    private fun updateNotification(region: String, steps: Int) {
        val notification = createNotification(region, steps)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(region: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 Animal Caught!")
            .setContentText("You've walked ${HuntFragment.STEPS_REQUIRED} steps in $region!")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Congratulations! You've caught an animal! Open the app to see what you found.")
            )
            .setAutoCancel(false)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)

        // Clear notification when service stops
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}