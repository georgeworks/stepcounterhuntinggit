package com.example.stepcounterhunting

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {
    private val binder = LocalBinder()
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var listener: StepCountListener? = null

    interface StepCountListener {
        fun onStepCountChanged(steps: Int)
        fun onAnimalCaught(animal: Animal)
    }

    inner class LocalBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        createNotificationChannel()
        startForegroundService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Counting steps for animal hunting"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hunting in Progress")
            .setContentText("Counting your steps...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun setStepCountListener(listener: StepCountListener?) {
        this.listener = listener
    }

    fun startCounting() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopCounting() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val huntState = HuntStateManager.getHuntState(this)
            if (huntState.isHunting) {
                if (huntState.initialStepCount < 0) {
                    HuntStateManager.updateInitialStepCount(this, event.values[0].toInt())
                }

                val currentSteps = event.values[0].toInt() - huntState.initialStepCount
                HuntStateManager.updateStepCount(this, currentSteps)

                listener?.onStepCountChanged(currentSteps)

                if (currentSteps >= STEPS_REQUIRED) {
                    catchAnimal()
                }
            }
        }
    }

    private fun catchAnimal() {
        val huntState = HuntStateManager.getHuntState(this)
        huntState.currentRegion?.let { region ->
            val caughtAnimal = selectRandomAnimal(region.animals)
            DataManager.addToCollection(caughtAnimal)
            HuntStateManager.resetStepsAfterCatch(this)
            listener?.onAnimalCaught(caughtAnimal)
        }
    }

    private fun selectRandomAnimal(animals: List<Animal>): Animal {
        val totalWeight = animals.sumOf { it.rarity.weight }
        var random = kotlin.random.Random.nextInt(totalWeight)

        for (animal in animals) {
            random -= animal.rarity.weight
            if (random < 0) {
                return animal
            }
        }

        return animals.last()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        stopCounting()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIFICATION_ID = 1
        const val STEPS_REQUIRED = 20
    }
}