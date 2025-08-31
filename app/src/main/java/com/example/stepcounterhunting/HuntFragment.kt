package com.example.stepcounterhunting

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

    private var isHunting = false
    private var stepCount = 0
    private var initialStepCount = -1
    private var currentRegion: Region? = null
    private var hasCompletedCurrentHunt = false  // Add this flag
    private var isShowingDialog = false  // Add this flag to prevent multiple dialogs

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

        setupSpinners()
        restoreHuntState()

        startHuntButton.setOnClickListener {
            if (isHunting) {
                stopHunting()
            } else {
                startHunting()
            }
        }

        progressBar.max = STEPS_REQUIRED
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

                // Update status text based on completion
                huntStatusText.text = if (hasCompletedCurrentHunt) {
                    "Goal reached! Continue hunting or stop to reset."
                } else {
                    "Hunt in progress!"
                }

                // Re-register sensor listener only if hunt not completed
                if (!hasCompletedCurrentHunt) {
                    stepSensor?.let {
                        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
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
            .apply()

        startHuntButton.text = "Stop Hunting"
        currentRegionText.text = "Hunting in: $selectedRegionName"
        huntStatusText.text = "Hunt started! Walk 10,000 steps to catch an animal!"

        // Register sensor listener
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

            // Save current steps
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

    private fun catchAnimal() {
        // Prevent multiple catches
        if (hasCompletedCurrentHunt || isShowingDialog) {
            return
        }

        hasCompletedCurrentHunt = true
        isShowingDialog = true

        // Save completion state
        prefs.edit().putBoolean("hunt_completed", true).apply()

        // Unregister sensor to stop counting
        sensorManager?.unregisterListener(this)

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
            .apply()

        // Re-register sensor listener
        if (isHunting) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            huntStatusText.text = "Continue hunting! Walk another 10,000 steps for the next animal!"
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
        if (isHunting && !hasCompletedCurrentHunt) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        isShowingDialog = false  // Reset dialog flag when resuming
    }

    companion object {
        const val STEPS_REQUIRED = 100  // Set to 100 for testing, change to 10000 for production
    }
}