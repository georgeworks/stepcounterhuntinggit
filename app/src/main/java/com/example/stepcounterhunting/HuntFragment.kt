// HuntFragment.kt - STABLE VERSION (Service optional)
package com.example.stepcounterhunting

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.random.Random

class HuntFragment : Fragment(), SensorEventListener {
    private lateinit var countrySpinner: Spinner
    private lateinit var regionSpinner: Spinner
    private lateinit var startHuntButton: Button
    private lateinit var stepCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var currentRegionText: TextView
    private lateinit var huntStatusText: TextView

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager

    private var isHunting = false
    private var stepCount = 0
    private var initialStepCount = -1
    private var currentRegion: Region? = null
    private var hasCompletedCurrentHunt = false
    private var isShowingDialog = false

    companion object {
        const val STEPS_REQUIRED = 100  // Set low for testing, change to 10000 for production
        const val CHANNEL_ID = "StepHuntingChannel"
        const val NOTIFICATION_ID = 2001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hunt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        countrySpinner = view.findViewById(R.id.country_spinner)
        regionSpinner = view.findViewById(R.id.region_spinner)
        startHuntButton = view.findViewById(R.id.start_hunt_button)
        stepCountText = view.findViewById(R.id.step_count_text)
        progressBar = view.findViewById(R.id.progress_bar)
        currentRegionText = view.findViewById(R.id.current_region_text)
        huntStatusText = view.findViewById(R.id.hunt_status_text)

        // Initialize components
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        createNotificationChannel()

        setupSpinners()
        restoreHuntState()

        startHuntButton.setOnClickListener {
            if (isHunting) {
                stopHunting()
            } else {
                if (checkPermissions()) {
                    startHunting()
                } else {
                    requestPermissions()
                }
            }
        }

        progressBar.max = STEPS_REQUIRED
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                1001
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Hunting Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your hunting progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupSpinners() {
        val countries = listOf("United States", "Canada", "Mexico", "Brazil", "United Kingdom")
        val countryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRegionSpinner(countries[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateRegionSpinner(country: String) {
        val regions = when (country) {
            "United States" -> DataManager.usRegions.map { it.name }
            "Canada" -> listOf("Western Canada", "Central Canada", "Atlantic Canada", "Northern Territories")
            "Mexico" -> listOf("Northern Mexico", "Central Mexico", "Southern Mexico")
            "Brazil" -> listOf("North", "Northeast", "Central-West", "Southeast", "South")
            "United Kingdom" -> listOf("England", "Scotland", "Wales", "Northern Ireland")
            else -> listOf()
        }

        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = regionAdapter
    }

    private fun restoreHuntState() {
        isHunting = prefs.getBoolean("is_hunting", false)
        stepCount = prefs.getInt("current_steps", 0)
        initialStepCount = prefs.getInt("initial_step_count", -1)
        hasCompletedCurrentHunt = prefs.getBoolean("hunt_completed", false)

        if (isHunting) {
            val savedCountry = prefs.getString("current_country", "") ?: ""
            val savedRegion = prefs.getString("current_region", "") ?: ""

            if (savedCountry.isNotEmpty() && savedRegion.isNotEmpty()) {
                // Restore spinner selections
                val countryAdapter = countrySpinner.adapter
                for (i in 0 until countryAdapter.count) {
                    if (countryAdapter.getItem(i) == savedCountry) {
                        countrySpinner.setSelection(i)
                        break
                    }
                }

                countrySpinner.post {
                    val regionAdapter = regionSpinner.adapter
                    if (regionAdapter != null) {
                        for (i in 0 until regionAdapter.count) {
                            if (regionAdapter.getItem(i) == savedRegion) {
                                regionSpinner.setSelection(i)
                                break
                            }
                        }
                    }
                }

                currentRegion = when (savedCountry) {
                    "United States" -> DataManager.usRegions.find { it.name == savedRegion }
                    else -> Region(savedRegion, DataManager.getDefaultAnimals())
                }

                startHuntButton.text = "Stop Hunting"
                currentRegionText.text = "Hunting in: $savedRegion"
                huntStatusText.text = if (hasCompletedCurrentHunt) {
                    "Goal reached! Continue hunting or stop to reset."
                } else {
                    "Hunt in progress!"
                }

                if (!hasCompletedCurrentHunt) {
                    stepSensor?.let {
                        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }

                // Show notification if hunting
                updateNotification()
            } else {
                isHunting = false
                prefs.edit().putBoolean("is_hunting", false).apply()
            }
        }

        updateUI()
    }

    private fun startHunting() {
        val selectedCountry = countrySpinner.selectedItem as? String
        val selectedRegionName = regionSpinner.selectedItem as? String

        if (selectedCountry == null || selectedRegionName == null) {
            Toast.makeText(context, "Please select a country and region", Toast.LENGTH_SHORT).show()
            return
        }

        currentRegion = when (selectedCountry) {
            "United States" -> DataManager.usRegions.find { it.name == selectedRegionName }
            else -> Region(selectedRegionName, DataManager.getDefaultAnimals())
        }

        isHunting = true
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        prefs.edit()
            .putBoolean("is_hunting", true)
            .putString("current_country", selectedCountry)
            .putString("current_region", selectedRegionName)
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .apply()

        startHuntButton.text = "Stop Hunting"
        currentRegionText.text = "Hunting in: $selectedRegionName"
        huntStatusText.text = "Hunt started! Walk $STEPS_REQUIRED steps to catch an animal!"

        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        updateNotification()
        updateUI()
    }

    private fun stopHunting() {
        isHunting = false
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        prefs.edit()
            .putBoolean("is_hunting", false)
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .remove("current_country")
            .remove("current_region")
            .apply()

        startHuntButton.text = "Start Hunting"
        huntStatusText.text = "Hunt stopped"
        currentRegionText.text = ""

        sensorManager?.unregisterListener(this)

        // Cancel notification
        notificationManager.cancel(NOTIFICATION_ID)

        updateUI()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isHunting && !hasCompletedCurrentHunt && event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit().putInt("initial_step_count", initialStepCount).apply()
            }

            stepCount = event.values[0].toInt() - initialStepCount
            prefs.edit().putInt("current_steps", stepCount).apply()

            if (stepCount >= STEPS_REQUIRED && !hasCompletedCurrentHunt && !isShowingDialog) {
                catchAnimal()
            }

            updateUI()
            updateNotification()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateUI() {
        val displaySteps = if (hasCompletedCurrentHunt) {
            STEPS_REQUIRED
        } else {
            stepCount.coerceAtMost(STEPS_REQUIRED)
        }

        stepCountText.text = "Steps: $displaySteps / $STEPS_REQUIRED"
        progressBar.progress = displaySteps

        if (hasCompletedCurrentHunt && isHunting) {
            huntStatusText.text = "Goal reached! Continue hunting for another animal or stop to reset."
        }
    }

    private fun updateNotification() {
        if (!isHunting) return

        try {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
            val progress = (stepCount * 100) / STEPS_REQUIRED
            val stepsRemaining = (STEPS_REQUIRED - stepCount).coerceAtLeast(0)

            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("🦌 Hunting in $region")
                .setContentText("Progress: $stepCount / $STEPS_REQUIRED steps")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(STEPS_REQUIRED, stepCount, false)
                .setSubText("$stepsRemaining steps to go • ${progress}% complete")
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Handle notification errors silently
        }
    }

    private fun catchAnimal() {
        if (hasCompletedCurrentHunt || isShowingDialog) return

        hasCompletedCurrentHunt = true
        isShowingDialog = true

        prefs.edit().putBoolean("hunt_completed", true).apply()
        sensorManager?.unregisterListener(this)

        currentRegion?.let { region ->
            val caughtAnimal = selectRandomAnimal(region.animals)
            DataManager.addToCollection(caughtAnimal)
            DataManager.addExploredRegion(region.name)

            val totalSteps = prefs.getInt("total_lifetime_steps", 0)
            prefs.edit()
                .putInt("total_lifetime_steps", totalSteps + STEPS_REQUIRED)
                .apply()

            val dialog = AnimalCaughtDialog(caughtAnimal) {
                continueHunting()
            }
            dialog.show(childFragmentManager, "animal_caught")
        }

        // Update notification to show completion
        try {
            val intent = Intent(requireContext(), MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val completionNotification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("🎉 Animal Caught!")
                .setContentText("You've caught an animal! Tap to see what you found.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(NOTIFICATION_ID, completionNotification)
        } catch (e: Exception) {
            // Handle notification errors
        }
    }

    private fun continueHunting() {
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        prefs.edit()
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .apply()

        if (isHunting) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            huntStatusText.text = "Continue hunting! Walk another $STEPS_REQUIRED steps for the next animal!"
        }

        updateNotification()
        updateUI()
    }

    private fun selectRandomAnimal(animals: List<Animal>): Animal {
        val totalWeight = animals.sumOf { it.rarity.weight }
        var random = Random.nextInt(totalWeight)

        for (animal in animals) {
            random -= animal.rarity.weight
            if (random < 0) {
                return animal
            }
        }

        return animals.last()
    }

    override fun onPause() {
        super.onPause()
        if (isHunting && !hasCompletedCurrentHunt) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHunting && !hasCompletedCurrentHunt) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        isShowingDialog = false
    }
}