package com.example.stepcounterhunting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class HuntFragment : Fragment(), StepCounterService.StepCountListener {
    private lateinit var countrySpinner: Spinner
    private lateinit var regionSpinner: Spinner
    private lateinit var startHuntButton: Button
    private lateinit var stepCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var currentRegionText: TextView
    private lateinit var huntStatusText: TextView

    private var stepCounterService: StepCounterService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StepCounterService.LocalBinder
            stepCounterService = binder.getService()
            stepCounterService?.setStepCountListener(this@HuntFragment)
            serviceBound = true
            updateUIFromSavedState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
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

        progressBar.max = StepCounterService.STEPS_REQUIRED

        setupSpinners()

        startHuntButton.setOnClickListener {
            val huntState = HuntStateManager.getHuntState(requireContext())
            if (huntState.isHunting) {
                stopHunting()
            } else {
                startHunting()
            }
        }

        // Bind to service
        Intent(requireContext(), StepCounterService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        // Update UI from saved state
        updateUIFromSavedState()
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

    private fun updateUIFromSavedState() {
        val huntState = HuntStateManager.getHuntState(requireContext())

        if (huntState.isHunting) {
            startHuntButton.text = "Stop Hunting"
            currentRegionText.text = "Hunting in: ${huntState.currentRegion?.name}"
            huntStatusText.text = "Hunt in progress! Keep walking!"
            stepCountText.text = "Steps: ${huntState.stepCount} / ${StepCounterService.STEPS_REQUIRED}"
            progressBar.progress = huntState.stepCount

            // Disable spinners during hunt
            countrySpinner.isEnabled = false
            regionSpinner.isEnabled = false
        } else {
            startHuntButton.text = "Start Hunting"
            currentRegionText.text = ""
            huntStatusText.text = "Select a region and start hunting"
            stepCountText.text = "Steps: 0 / ${StepCounterService.STEPS_REQUIRED}"
            progressBar.progress = 0

            // Enable spinners
            countrySpinner.isEnabled = true
            regionSpinner.isEnabled = true
        }
    }

    private fun startHunting() {
        val selectedCountry = countrySpinner.selectedItem as String
        val selectedRegionName = regionSpinner.selectedItem as String

        val currentRegion = when (selectedCountry) {
            "United States" -> DataManager.usRegions.find { it.name == selectedRegionName }
            else -> Region(selectedRegionName, DataManager.getDefaultAnimals())
        }

        currentRegion?.let { region ->
            HuntStateManager.startHunt(requireContext(), selectedCountry, region)

            // Start service if not running
            val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }

            stepCounterService?.startCounting()

            updateUIFromSavedState()
        }
    }

    private fun stopHunting() {
        HuntStateManager.stopHunt(requireContext())
        stepCounterService?.stopCounting()

        // Stop the service
        requireContext().stopService(Intent(requireContext(), StepCounterService::class.java))

        updateUIFromSavedState()
    }

    override fun onStepCountChanged(steps: Int) {
        activity?.runOnUiThread {
            stepCountText.text = "Steps: $steps / ${StepCounterService.STEPS_REQUIRED}"
            progressBar.progress = steps.coerceAtMost(StepCounterService.STEPS_REQUIRED)
        }
    }

    override fun onAnimalCaught(animal: Animal) {
        activity?.runOnUiThread {
            // Show catch dialog
            val dialog = AnimalCaughtDialog(animal) {
                updateUIFromSavedState()
            }
            dialog.show(childFragmentManager, "animal_caught")
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIFromSavedState()

        // Re-bind to service if needed
        if (!serviceBound) {
            Intent(requireContext(), StepCounterService::class.java).also { intent ->
                requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (serviceBound) {
            stepCounterService?.setStepCountListener(null)
            requireContext().unbindService(serviceConnection)
            serviceBound = false
        }
    }
}