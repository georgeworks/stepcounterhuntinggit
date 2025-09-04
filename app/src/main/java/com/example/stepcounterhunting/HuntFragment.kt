// HuntFragment.kt - WITH SIMPLE NOTIFICATION
package com.example.stepcounterhunting

import android.Manifest
import android.app.AlertDialog
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
    private var selectedCountry: String? = null
    private var selectedRegionName: String? = null
    private var huntingCountry: String? = null
    private var huntingRegionName: String? = null
    private var lastNotificationUpdate = 0L
    private var isUsingLure = false
    private lateinit var lureCountText: TextView
    private lateinit var regionProgressText: TextView

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
        lureCountText = view.findViewById(R.id.lure_count_text)
        countrySpinner = view.findViewById(R.id.country_spinner)
        regionSpinner = view.findViewById(R.id.region_spinner)
        startHuntButton = view.findViewById(R.id.start_hunt_button)
        stepCountText = view.findViewById(R.id.step_count_text)
        progressBar = view.findViewById(R.id.progress_bar)
        currentRegionText = view.findViewById(R.id.current_region_text)
        huntStatusText = view.findViewById(R.id.hunt_status_text)
        regionProgressText = view.findViewById(R.id.region_progress_text)
        updateLureDisplay()
        // Initialize sensor and preferences
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

        // Initialize notification manager if we have permission
        if (hasNotificationPermission()) {
            notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel()
        }

        setupSpinners()
        restoreHuntState()

        startHuntButton.setOnClickListener {
            when {
                !isHunting -> {
                    // Not hunting, start a new hunt
                    if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission()
                    }
                    startHunting()
                }

                isHunting && (selectedCountry != huntingCountry || selectedRegionName != huntingRegionName) -> {
                    // Hunting but selected different region, show warning
                    showRegionChangeDialog()
                }

                else -> {
                    // Normal stop hunting
                    stopHunting()
                }
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
        val countryAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedCountry = countries[position]
                updateRegionSpinner(countries[position])
                checkForRegionChange()

                // Save the country selection as preference
                prefs.edit().putString("last_selected_country", selectedCountry).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateRegionSpinner(country: String) {
        val regions = when (country) {
            "United States" -> DataManager.usRegions.map { it.name }
            "Canada" -> listOf(
                "Western Canada",
                "Central Canada",
                "Atlantic Canada",
                "Northern Territories"
            )
            "Mexico" -> listOf("Northern Mexico", "Central Mexico", "Southern Mexico")
            "Brazil" -> listOf("North", "Northeast", "Central-West", "Southeast", "South")
            "United Kingdom" -> listOf("England", "Scotland", "Wales", "Northern Ireland")
            else -> listOf()
        }

        val regionAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = regionAdapter

        regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedRegionName = regions[position]
                checkForRegionChange()
                updateRegionProgress()

                // Save the region selection as preference for this country
                prefs.edit().putString("last_selected_region_$country", selectedRegionName).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Try to restore the last selected region for this country
        val currentHuntRegion = if (prefs.getString("current_country", null) == country) {
            prefs.getString("current_region", null)
        } else null

        val lastSelectedRegion = prefs.getString("last_selected_region_$country", null)
        val defaultRegion = currentHuntRegion ?: lastSelectedRegion

        // Set the spinner to the preferred region if it exists
        if (defaultRegion != null && regions.contains(defaultRegion)) {
            val regionIndex = regions.indexOf(defaultRegion)
            if (regionIndex >= 0) {
                regionSpinner.setSelection(regionIndex)
            }
        }

        updateRegionProgress()
    }

    private fun checkForRegionChange() {
        if (isHunting && !hasCompletedCurrentHunt) {
            // Check if the selected region is different from the hunting region
            if ((selectedCountry != huntingCountry || selectedRegionName != huntingRegionName)
                && selectedCountry != null && selectedRegionName != null
            ) {
                // User selected a different region while hunting
                startHuntButton.text = "Change Region?"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_orange_dark
                    )
                )
            } else {
                // Same region selected, show normal stop button
                startHuntButton.text = "Stop Hunting"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
                )
            }
        }
    }

    private fun updateRegionProgress() {
        val selectedCountry = countrySpinner.selectedItem as? String
        val selectedRegionName = regionSpinner.selectedItem as? String

        if (selectedCountry == null || selectedRegionName == null) {
            regionProgressText.text = "0/?"
            return
        }

        // Get all caught animals
        val caughtAnimals = DataManager.getCollection()
        val uniqueCaughtAnimals = caughtAnimals.distinctBy { it.id }

        when (selectedCountry) {
            "United States" -> {
                // Find the selected US region
                val selectedRegion = DataManager.usRegions.find { it.name == selectedRegionName }
                if (selectedRegion != null) {
                    // Count how many animals from this region the player has caught
                    val caughtInRegion = uniqueCaughtAnimals.count { caughtAnimal ->
                        selectedRegion.animals.any { it.id == caughtAnimal.id }
                    }
                    val totalInRegion = selectedRegion.animals.size

                    regionProgressText.text = "$caughtInRegion/$totalInRegion"

                    // Optional: Change color based on progress
                    regionProgressText.setTextColor(when {
                        caughtInRegion == totalInRegion ->
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        caughtInRegion >= totalInRegion / 2 ->
                            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                        else ->
                            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                    })
                } else {
                    regionProgressText.text = "0/10"
                }
            }
            else -> {
                // For non-US countries, we use default animals (assuming 5 per country)
                val caughtInCountry = uniqueCaughtAnimals.count { animal ->
                    when (selectedCountry) {
                        "Canada" -> animal.region.contains("Canada") &&
                                (selectedRegionName in animal.region)
                        "Mexico" -> animal.region.contains("Mexico") &&
                                (selectedRegionName in animal.region)
                        "Brazil" -> animal.region.contains("Brazil") &&
                                (selectedRegionName in animal.region)
                        "United Kingdom" -> (animal.region.contains("United Kingdom") ||
                                animal.region.contains(selectedRegionName))
                        else -> false
                    }
                }

                // Assume 5 animals per region for non-US countries
                regionProgressText.text = "$caughtInCountry/5"

                // Optional: Change color based on progress
                regionProgressText.setTextColor(when {
                    caughtInCountry >= 5 ->
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    caughtInCountry >= 3 ->
                        ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                    else ->
                        ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                })
            }
        }
    }

    private fun restoreHuntState() {
        // Check if this is first app launch
        val isFirstLaunch = prefs.getBoolean("first_launch_complete", true)
        if (isFirstLaunch) {
            // First time opening app - ensure clean state
            prefs.edit()
                .putBoolean("first_launch_complete", false)
                .putBoolean("is_hunting", false)
                .putInt("current_steps", 0)
                .putInt("initial_step_count", -1)
                .putBoolean("hunt_completed", false)
                .remove("current_country")
                .remove("current_region")
                .apply()

            val dataPrefs = requireContext().getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)
            dataPrefs.edit()
                .putInt("lure_count", 0)
                .putString("collection", "")
                .putString("explored_regions", "")
                .apply()
        }

        isHunting = prefs.getBoolean("is_hunting", false)
        stepCount = prefs.getInt("current_steps", 0)
        initialStepCount = prefs.getInt("initial_step_count", -1)

        // Check if service marked hunt as completed while app was closed
        val serviceCompletedHunt = prefs.getBoolean("hunt_completed", false)
        val needsToShowCatch =
            serviceCompletedHunt && !hasCompletedCurrentHunt && stepCount >= STEPS_REQUIRED
        isUsingLure = prefs.getBoolean("using_lure", false)

        if (isUsingLure && isHunting) {  // Only show lure active if actually hunting
            huntStatusText.text = "🎯 LURE ACTIVE! Hunt in progress!"
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        }

        if (isHunting) {
            val savedCountry = prefs.getString("current_country", "") ?: ""
            val savedRegion = prefs.getString("current_region", "") ?: ""

            if (savedCountry.isNotEmpty() && savedRegion.isNotEmpty()) {
                // Restore hunting region info
                huntingCountry = savedCountry
                huntingRegionName = savedRegion
                selectedCountry = savedCountry
                selectedRegionName = savedRegion

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
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
                )
                currentRegionText.text = "Hunting in: $savedRegion"

                // Check if we need to show the catch dialog
                if (needsToShowCatch) {
                    huntStatusText.text = "Goal reached! Opening your catch..."
                    view?.postDelayed({
                        if (!isShowingDialog && currentRegion != null) {
                            catchAnimal()
                        }
                    }, 500)
                } else if (serviceCompletedHunt) {
                    hasCompletedCurrentHunt = true
                    huntStatusText.text = "Goal reached! Continue hunting or stop to reset."
                } else {
                    huntStatusText.text = "Hunt in progress!"
                    stepSensor?.let {
                        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }

                if (!serviceCompletedHunt) {
                    showNotification()
                }
            } else {
                // Invalid saved state, reset
                isHunting = false
                prefs.edit().putBoolean("is_hunting", false).apply()

                // Set UI to not hunting state
                startHuntButton.text = "Start Hunting"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_green_dark
                    )
                )
            }
        } else {
            // Not hunting - ensure UI is in correct state
            startHuntButton.text = "Start Hunting"
            startHuntButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.holo_green_dark
                )
            )
            currentRegionText.text = ""
            huntStatusText.text = ""

            // Set spinners to last selected or default
            val lastCountry = prefs.getString("last_selected_country", null)
            val defaultCountry = lastCountry ?: "United States"

            val countryAdapter = countrySpinner.adapter
            if (countryAdapter != null) {
                for (i in 0 until countryAdapter.count) {
                    if (countryAdapter.getItem(i) == defaultCountry) {
                        countrySpinner.setSelection(i)

                        countrySpinner.post {
                            val lastRegion = prefs.getString("last_selected_region_$defaultCountry", null)

                            if (lastRegion != null) {
                                val regionAdapter = regionSpinner.adapter
                                if (regionAdapter != null) {
                                    for (j in 0 until regionAdapter.count) {
                                        if (regionAdapter.getItem(j) == lastRegion) {
                                            regionSpinner.setSelection(j)
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        break
                    }
                }
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
        // Check for lures and ask if they want to use one
        val lureCount = DataManager.getLureCount()
        if (lureCount > 0) {
            showLureDialog(selectedCountry, selectedRegionName)
        } else {
            startHuntingWithLure(selectedCountry, selectedRegionName, false)
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

        // Save the hunting region
        huntingCountry = selectedCountry
        huntingRegionName = selectedRegionName
        this.selectedCountry = selectedCountry
        this.selectedRegionName = selectedRegionName

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
        startHuntButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_light
            )
        )
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

    private fun showLureDialog(selectedCountry: String, selectedRegionName: String) {
        val lureCount = DataManager.getLureCount()

        AlertDialog.Builder(requireContext())
            .setTitle("Use a Lure?")
            .setMessage("You have $lureCount lure${if (lureCount > 1) "s" else ""}.\n\nUsing a lure guarantees your next catch will be Rare or better!\n\nWould you like to use one?")
            .setPositiveButton("Yes, Use Lure") { _, _ ->
                DataManager.useLure()
                startHuntingWithLure(selectedCountry, selectedRegionName, true)
                updateLureDisplay()
            }
            .setNegativeButton("No Thanks") { _, _ ->
                startHuntingWithLure(selectedCountry, selectedRegionName, false)
            }
            .setCancelable(true)
            .show()
    }

    private fun startHuntingWithLure(
        selectedCountry: String,
        selectedRegionName: String,
        useLure: Boolean
    ) {
        currentRegion = when (selectedCountry) {
            "United States" -> DataManager.usRegions.find { it.name == selectedRegionName }
            else -> Region(selectedRegionName, DataManager.getDefaultAnimals())
        }

        isHunting = true
        isUsingLure = useLure
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        // Save the hunting region
        huntingCountry = selectedCountry
        huntingRegionName = selectedRegionName
        this.selectedCountry = selectedCountry
        this.selectedRegionName = selectedRegionName

        // Save hunting state (add lure status)
        prefs.edit()
            .putBoolean("is_hunting", true)
            .putString("current_country", selectedCountry)
            .putString("current_region", selectedRegionName)
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .putBoolean("catch_processed", false)
            .putBoolean("using_lure", useLure)  // Save lure status
            .apply()

        startHuntButton.text = "Stop Hunting"
        startHuntButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_light
            )
        )
        currentRegionText.text = "Hunting in: $selectedRegionName"

        // Update status text to show lure is active
        huntStatusText.text = if (useLure) {
            "🎯 LURE ACTIVE! Walk $STEPS_REQUIRED steps for a rare+ animal!"
        } else {
            "Hunt started! Walk $STEPS_REQUIRED steps to catch an animal!"
        }

        // Change UI color if lure is active (optional)
        if (useLure) {
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        } else {
            progressBar.progressTintList = null
        }

        // Start the foreground service for background counting
        try {
            StepCounterService.startService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        updateUI()
    }

    private fun showRegionChangeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Region?")
            .setMessage("You have $stepCount steps in ${huntingRegionName}.\n\nChanging regions will lose your current progress. Are you sure you want to switch to ${selectedRegionName}?")
            .setPositiveButton("Yes, Change Region") { _, _ ->
                // Stop current hunt and start new one
                stopHunting()
                startHunting()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Reset spinners to current hunting region
                val countryAdapter = countrySpinner.adapter
                for (i in 0 until countryAdapter.count) {
                    if (countryAdapter.getItem(i) == huntingCountry) {
                        countrySpinner.setSelection(i)
                        break
                    }
                }

                countrySpinner.post {
                    val regionAdapter = regionSpinner.adapter
                    if (regionAdapter != null) {
                        for (i in 0 until regionAdapter.count) {
                            if (regionAdapter.getItem(i) == huntingRegionName) {
                                regionSpinner.setSelection(i)
                                break
                            }
                        }
                    }
                }

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun stopHunting() {
        isHunting = false
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false
        huntingCountry = null
        huntingRegionName = null

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
        startHuntButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_green_dark
            )
        )
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
                // Important: Set step count to 0 on initialization
                stepCount = 0
                prefs.edit().putInt("current_steps", 0).apply()
                updateUI()
                return // Exit early on initialization
            }

            stepCount = event.values[0].toInt() - initialStepCount

            // Protection against negative values (can happen after reinstall)
            if (stepCount < 0) {
                // Reset the initial count to current sensor value
                initialStepCount = event.values[0].toInt()
                stepCount = 0
                prefs.edit()
                    .putInt("initial_step_count", initialStepCount)
                    .putInt("current_steps", 0)
                    .apply()
            } else {
                // Save current steps only if valid
                prefs.edit().putInt("current_steps", stepCount).apply()
            }

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
            huntStatusText.text =
                "Goal reached! Continue hunting for another animal or stop to reset."
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
        if (hasCompletedCurrentHunt || isShowingDialog) {
            return
        }

        val catchProcessed = prefs.getBoolean("catch_processed", false)
        if (catchProcessed) {
            hasCompletedCurrentHunt = true
            return
        }

        hasCompletedCurrentHunt = true
        isShowingDialog = true

        prefs.edit()
            .putBoolean("hunt_completed", true)
            .putBoolean("catch_processed", true)
            .apply()

        sensorManager?.unregisterListener(this)

        try {
            StepCounterService.stopService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        currentRegion?.let { region ->
            // Get the caught animal based on lure status
            val caughtAnimal = if (isUsingLure) {
                selectRareAnimal(region.animals)
            } else {
                selectRandomAnimal(region.animals)
            }

            // Check if it's a duplicate and add to collection
            val isDuplicate = DataManager.addToCollection(caughtAnimal)
            DataManager.addExploredRegion(region.name)

            val totalSteps = prefs.getInt("total_lifetime_steps", 0)
            prefs.edit()
                .putInt("total_lifetime_steps", totalSteps + STEPS_REQUIRED)
                .apply()

            // Show catch dialog with duplicate indicator
            val dialog = AnimalCaughtDialogWithLure(caughtAnimal, isDuplicate, isUsingLure) {
                // Update lure display BEFORE continuing hunt
                activity?.runOnUiThread {
                    updateLureDisplay()
                }
                continueHunting()
            }
            dialog.show(childFragmentManager, "animal_caught")

            // Reset lure status
            isUsingLure = false
            prefs.edit().putBoolean("using_lure", false).apply()

            // Force immediate UI update after dialog shows
            activity?.runOnUiThread {
                updateLureDisplay()
            }
        }
    }

    private fun selectRareAnimal(animals: List<Animal>): Animal {
        // Filter to only Rare, Epic, and Legendary
        val rareAnimals = animals.filter {
            it.rarity == Rarity.RARE ||
                    it.rarity == Rarity.EPIC ||
                    it.rarity == Rarity.LEGENDARY
        }

        // If no rare animals in this region (shouldn't happen), fall back to normal selection
        if (rareAnimals.isEmpty()) {
            return selectRandomAnimal(animals)
        }

        // Select from rare animals with weighted probability
        val totalWeight = rareAnimals.sumOf { it.rarity.weight }
        var random = Random.nextInt(totalWeight)

        for (animal in rareAnimals) {
            random -= animal.rarity.weight
            if (random < 0) {
                return animal
            }
        }

        return rareAnimals.last()
    }

    // Add function to update lure display:
    private fun updateLureDisplay() {
        val lureCount = DataManager.getLureCount()
        lureCountText.text = "Lures: $lureCount"

        // Always show the lure counter (remove the visibility condition)
        lureCountText.visibility = View.VISIBLE

        // Force layout update
        lureCountText.parent?.requestLayout()
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
            huntStatusText.text =
                "Continue hunting! Walk another $STEPS_REQUIRED steps for the next animal!"

            // Restart the service for the next hunt
            try {
                StepCounterService.startService(requireContext())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Update lure display after continuing
        updateLureDisplay()
        updateRegionProgress()
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

        // Always update lure display when fragment resumes
        updateLureDisplay()
        updateRegionProgress()

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