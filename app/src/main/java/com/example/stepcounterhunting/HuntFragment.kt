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
    private var notificationManager: NotificationManager? = null

    private var isHunting = false
    private var stepCount = 0
    private var initialStepCount = -1
    private var currentRegion: Region? = null
    private var hasCompletedCurrentHunt = false
    private var isShowingDialog = false
    private var lastNotificationUpdate = 0L

    companion object {
        const val STEPS_REQUIRED = 100  // Set low for testing
        const val CHANNEL_ID = "StepHuntingChannel"
        const val NOTIFICATION_ID = 2001
        const val NOTIFICATION_UPDATE_INTERVAL = 10 // Update notification every 10 steps
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

        // Initialize sensor and preferences
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

        // Initialize notification manager if we have permission
        if (hasNotificationPermission()) {
            notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel()
        }

        setupSpinners()
        restoreHuntState()

        startHuntButton.setOnClickListener {
            if (isHunting) {
                stopHunting()
            } else {
                // Check for notification permission first
                if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermission()
                }
                startHunting()
            }
        }

        progressBar.max = STEPS_REQUIRED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed below Android 13
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1002
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
                setSound(null, null) // No sound for updates
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupSpinners() {
        // Setup country spinner
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

        // Check if service marked hunt as completed while app was closed
        val serviceCompletedHunt = prefs.getBoolean("hunt_completed", false)
        val needsToShowCatch = serviceCompletedHunt && !hasCompletedCurrentHunt && stepCount >= STEPS_REQUIRED

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

                // Wait for region spinner to populate
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

                // Check if we need to show the catch dialog
                if (needsToShowCatch) {
                    huntStatusText.text = "Goal reached! Opening your catch..."
                    // Don't set hasCompletedCurrentHunt yet - let catchAnimal do it
                    // Delay slightly to ensure UI is ready
                    view?.postDelayed({
                        if (!isShowingDialog && currentRegion != null) {
                            catchAnimal()
                        }
                    }, 500)
                } else if (serviceCompletedHunt) {
                    // Already shown before, just update state
                    hasCompletedCurrentHunt = true
                    huntStatusText.text = "Goal reached! Continue hunting or stop to reset."
                } else {
                    huntStatusText.text = "Hunt in progress!"
                    // Re-register sensor listener only if hunt not completed
                    stepSensor?.let {
                        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }

                // Show notification if hunting and not completed
                if (!serviceCompletedHunt) {
                    showNotification()
                }
            } else {
                // Invalid saved state, reset
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

        // Save hunting state
        prefs.edit()
            .putBoolean("is_hunting", true)
            .putString("current_country", selectedCountry)
            .putString("current_region", selectedRegionName)
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .putBoolean("catch_processed", false)  // Reset the processed flag
            .apply()

        startHuntButton.text = "Stop Hunting"
        currentRegionText.text = "Hunting in: $selectedRegionName"
        huntStatusText.text = "Hunt started! Walk $STEPS_REQUIRED steps to catch an animal!"

        // Start the foreground service for background counting
        try {
            StepCounterService.startService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
            // If service fails, still use local sensor
        }

        // Also register local sensor for immediate UI updates
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        updateUI()
    }

    private fun stopHunting() {
        isHunting = false
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        // Clear hunting state
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

        // Stop the service
        try {
            StepCounterService.stopService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateUI()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Only process if hunting and haven't completed this hunt
        if (isHunting && !hasCompletedCurrentHunt && event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit().putInt("initial_step_count", initialStepCount).apply()
            }

            stepCount = event.values[0].toInt() - initialStepCount

            // Save current steps (service will read these)
            prefs.edit().putInt("current_steps", stepCount).apply()

            // Check if we've reached the goal and haven't already caught an animal
            if (stepCount >= STEPS_REQUIRED && !hasCompletedCurrentHunt && !isShowingDialog) {
                catchAnimal()
            }

            updateUI()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateUI() {
        // Cap the displayed steps at the requirement if hunt is completed
        val displaySteps = if (hasCompletedCurrentHunt) {
            STEPS_REQUIRED
        } else {
            stepCount.coerceAtMost(STEPS_REQUIRED)
        }

        stepCountText.text = "Steps: $displaySteps / $STEPS_REQUIRED"
        progressBar.progress = displaySteps

        // Update status if goal reached but not yet reset
        if (hasCompletedCurrentHunt && isHunting) {
            huntStatusText.text = "Goal reached! Continue hunting for another animal or stop to reset."
        }
    }

    private fun showNotification() {
        // Only show notification if we have permission and manager
        if (notificationManager == null || !hasNotificationPermission()) {
            return
        }

        try {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val region = prefs.getString("current_region", "Unknown") ?: "Unknown"
            val currentSteps = prefs.getInt("current_steps", 0)
            val progress = (currentSteps * 100) / STEPS_REQUIRED
            val stepsRemaining = (STEPS_REQUIRED - currentSteps).coerceAtLeast(0)

            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("🦌 Hunting in $region")
                .setContentText("Progress: $currentSteps / $STEPS_REQUIRED steps")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes it persistent
                .setSilent(true) // No sound
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(STEPS_REQUIRED, currentSteps, false)
                .setSubText("$stepsRemaining steps to go • ${progress}% complete")
                .setOnlyAlertOnce(true) // Don't re-alert on updates
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Silently handle any notification errors
            e.printStackTrace()
        }
    }

    private fun showCompletionNotification() {
        if (notificationManager == null || !hasNotificationPermission()) {
            return
        }

        try {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("🎉 Animal Caught!")
                .setContentText("You've walked $STEPS_REQUIRED steps! Tap to see what you caught!")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Dismiss when tapped
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNotification() {
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun catchAnimal() {
        // Prevent multiple catches
        if (hasCompletedCurrentHunt || isShowingDialog) {
            return
        }

        // Check if we already processed this catch (for the current hunt session)
        val catchProcessed = prefs.getBoolean("catch_processed", false)
        if (catchProcessed) {
            hasCompletedCurrentHunt = true
            return
        }

        hasCompletedCurrentHunt = true
        isShowingDialog = true

        // Mark this catch as processed
        prefs.edit()
            .putBoolean("hunt_completed", true)
            .putBoolean("catch_processed", true)
            .apply()

        // Unregister sensor to stop counting
        sensorManager?.unregisterListener(this)

        // Stop the service since hunt is complete
        try {
            StepCounterService.stopService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        currentRegion?.let { region ->
            val caughtAnimal = selectRandomAnimal(region.animals)
            DataManager.addToCollection(caughtAnimal)
            DataManager.addExploredRegion(region.name)

            // Update total steps
            val totalSteps = prefs.getInt("total_lifetime_steps", 0)
            prefs.edit()
                .putInt("total_lifetime_steps", totalSteps + STEPS_REQUIRED)
                .apply()

            // Show catch dialog
            val dialog = AnimalCaughtDialog(caughtAnimal) {
                // Dialog dismissed - allow continuing or starting new hunt
                continueHunting()
            }
            dialog.show(childFragmentManager, "animal_caught")
        }
    }

    private fun continueHunting() {
        // Reset for next hunt in same region
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        prefs.edit()
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .putBoolean("catch_processed", false)  // Reset the processed flag
            .apply()

        // Re-register sensor listener
        if (isHunting) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            huntStatusText.text = "Continue hunting! Walk another $STEPS_REQUIRED steps for the next animal!"

            // Restart the service for the next hunt
            try {
                StepCounterService.startService(requireContext())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
        if (isHunting) {
            // Always check for latest step count from service
            val latestSteps = prefs.getInt("current_steps", 0)
            val huntCompleted = prefs.getBoolean("hunt_completed", false)

            // Update our local step count
            stepCount = latestSteps

            // Check if hunt was completed while we were away
            if (huntCompleted && !hasCompletedCurrentHunt && !isShowingDialog && stepCount >= STEPS_REQUIRED) {
                // Service completed the hunt while app was closed
                updateUI()
                // Trigger the catch dialog
                if (currentRegion != null) {
                    catchAnimal()
                }
            } else if (!hasCompletedCurrentHunt) {
                // Resume normal sensor listening if hunt not complete
                stepSensor?.let {
                    sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                updateUI()
            }
        }
        isShowingDialog = false  // Reset dialog flag when resuming
    }
}