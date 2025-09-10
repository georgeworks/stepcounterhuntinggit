// HuntFragment.kt - CLEANED VERSION
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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView

class HuntFragment : Fragment(), SensorEventListener {
    private lateinit var countryScrollView: HorizontalScrollView
    private lateinit var countryContainer: LinearLayout
    private lateinit var regionViewPager: ViewPager2
    private lateinit var pageIndicator: LinearLayout
    private lateinit var regionAdapter: RegionCardAdapter
    private lateinit var startHuntButton: Button
    private lateinit var stepCountText: TextView
    private lateinit var progressBar: ProgressBar
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
    private lateinit var streakContainer: LinearLayout
    private lateinit var streakBubblesContainer: LinearLayout
    private lateinit var totalStreakText: TextView
    private lateinit var streakLabel: TextView
    private var currentStreakDay = 0
    private var lastHuntDate: String = ""
    private val catchLock = Object()
    private var lastCatchAttempt = 0L
    private var isCatchInProgress = false

    private var tutorialOverlay: TutorialOverlay? = null
    private var hasPendingTutorial = false
    private var selectedCountryIndex = 0
    private var selectedRegion: Region? = null

    companion object {
        const val STEPS_REQUIRED = 100
        const val CHANNEL_ID = "StepHuntingChannel"
        const val NOTIFICATION_ID = 2001
        const val NOTIFICATION_UPDATE_INTERVAL = 10
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
        countryScrollView = view.findViewById(R.id.country_scroll_view)
        countryContainer = view.findViewById(R.id.country_container)
        regionViewPager = view.findViewById(R.id.region_view_pager)
        pageIndicator = view.findViewById(R.id.page_indicator)
        startHuntButton = view.findViewById(R.id.start_hunt_button)
        stepCountText = view.findViewById(R.id.step_count_text)
        progressBar = view.findViewById(R.id.progress_bar)
        huntStatusText = view.findViewById(R.id.hunt_status_text)
        streakContainer = view.findViewById(R.id.streak_container)
        streakBubblesContainer = view.findViewById(R.id.streak_bubbles_container)
        totalStreakText = view.findViewById(R.id.total_streak_text)
        streakLabel = view.findViewById(R.id.streak_label)
        // Removed: currentRegionText and regionProgressText

        // Initialize sensor and preferences
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

        // Check for actual first launch - check BOTH SharedPreferences
        val huntFragmentInitialized = prefs.getBoolean("app_initialized", false)
        val dataManagerPrefs = requireContext().getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)
        val dataManagerInitialized = dataManagerPrefs.getBoolean("app_initialized", false)

        val isFirstLaunch = !huntFragmentInitialized || !dataManagerInitialized

        // If EITHER is not initialized, treat as first launch and clear EVERYTHING
        if (isFirstLaunch) {
            // Clear all SharedPreferences
            prefs.edit().clear().apply()  // Clear first
            dataManagerPrefs.edit().clear().apply()  // Clear first

            // Clear streak data
            requireContext().getSharedPreferences("StreakData", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // Re-initialize DataManager to ensure it's clean
            DataManager.init(requireContext())

            // NOW set initialized flags after clearing
            prefs.edit()
                .putBoolean("app_initialized", true)
                .putBoolean("is_hunting", false)  // Explicitly set hunting to false
                .putInt("current_steps", 0)
                .putInt("initial_step_count", -1)
                .putBoolean("hunt_completed", false)
                .putBoolean("catch_processed", false)
                .putBoolean("using_lure", false)
                .apply()

            dataManagerPrefs.edit()
                .putBoolean("app_initialized", true)
                .apply()

            // Reset all state variables to ensure clean state
            isHunting = false
            stepCount = 0
            initialStepCount = -1
            hasCompletedCurrentHunt = false
            isShowingDialog = false
            isCatchInProgress = false
            currentRegion = null
            selectedRegion = null
            huntingCountry = null
            huntingRegionName = null
        }

        // Initialize notification manager if we have permission
        if (hasNotificationPermission()) {
            notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel()
        }

        // NOW restore hunt state - after clearing on first launch
        if (!isFirstLaunch) {
            restoreHuntState()
        } else {
            // On first launch, ensure clean UI state
            startHuntButton.text = "Start Hunting"
            startHuntButton.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
            huntStatusText.text = ""
            stepCountText.text = "Steps: 0 / $STEPS_REQUIRED"
            progressBar.progress = 0
        }

        // Then setup UI components with the correct initial state
        setupCountrySelector()
        setupRegionCards()

        // Only check for pending catch if both were already initialized
        if (!isFirstLaunch) {
            checkForPendingCatch()
        }

        // Update displays after everything is set up
        updateLureDisplay()
        updateStreakDisplay()

        startHuntButton.setOnClickListener {
            when {
                !isHunting -> {
                    if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission()
                    }
                    startHunting()
                }

                isHunting && (selectedCountry != huntingCountry || selectedRegionName != huntingRegionName) -> {
                    showRegionChangeDialog()
                }

                else -> {
                    stopHunting()
                }
            }
        }

        progressBar.max = STEPS_REQUIRED

        // Initialize tutorial overlay
        initializeTutorial()
    }
    private fun checkForPendingCatch() {
        val huntCompleted = prefs.getBoolean("hunt_completed", false)
        val catchProcessed = prefs.getBoolean("catch_processed", false)
        val currentSteps = prefs.getInt("current_steps", 0)

        // Remove the isHunting check to make it work when opening from notification
        if (huntCompleted && !catchProcessed && currentSteps >= STEPS_REQUIRED) {
            // Make sure we have a valid region
            if (currentRegion == null) {
                val savedRegion = prefs.getString("current_region", "") ?: ""
                currentRegion = DataManager.usRegions.find { it.name == savedRegion }
            }

            // Also restore hunting state if needed
            if (!isHunting) {
                val savedCountry = prefs.getString("current_country", "")
                val savedRegion = prefs.getString("current_region", "")
                if (!savedCountry.isNullOrEmpty() && !savedRegion.isNullOrEmpty()) {
                    isHunting = true
                    huntingCountry = savedCountry
                    huntingRegionName = savedRegion
                }
            }

            if (currentRegion != null) {
                // Force reset these flags before calling catchAnimal
                hasCompletedCurrentHunt = false
                isShowingDialog = false
                isCatchInProgress = false

                catchAnimal()
            }
        }
    }

    private fun setupCountrySelector() {
        val countries = listOf(
            Pair("United States", true),
            Pair("China", false),
            Pair("Australia", false),
            Pair("Brazil", false),
            Pair("Madagascar", false)
        )

        countries.forEachIndexed { index, (country, isEnabled) ->
            val bubbleView = layoutInflater.inflate(R.layout.item_country_bubble, countryContainer, false)
            val card = bubbleView.findViewById<CardView>(R.id.country_card)
            val nameText = bubbleView.findViewById<TextView>(R.id.country_name)
            val comingSoonText = bubbleView.findViewById<TextView>(R.id.coming_soon_text)

            nameText.text = country

            if (!isEnabled) {
                comingSoonText.visibility = View.VISIBLE
                card.alpha = 0.5f
                card.isClickable = false
            } else {
                card.setOnClickListener {
                    selectCountry(index)
                }
                // Select United States by default
                if (index == 0) {
                    card.cardElevation = 8f
                    selectedCountry = "United States"
                }
            }

            countryContainer.addView(bubbleView)
        }
    }

    private fun setupRegionCards() {
        val collection = DataManager.getCollection().toSet()

        // Determine the initial position BEFORE setting up the adapter
        val initialPosition = determineInitialRegionPosition()

        // Note: Now passing only collection, not collectedAnimals
        regionAdapter = RegionCardAdapter(
            DataManager.usRegions,
            collection  // Pass only the collection Set
        ) { region, position ->
            selectedRegion = region
            selectedRegionName = region.name
            selectedCountry = "United States"
            currentRegion = region
            checkForRegionChange()
        }

        regionViewPager.adapter = regionAdapter

        // Add page change listener for dots
        regionViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
                regionAdapter.setSelectedPosition(position)
                selectedRegion = DataManager.usRegions[position]
                selectedRegionName = selectedRegion?.name
                selectedCountry = "United States"
                currentRegion = selectedRegion
                checkForRegionChange()
            }
        })

        // Setup page indicator dots
        setupPageIndicator(DataManager.usRegions.size)

        // Set the initial position WITHOUT animation
        regionViewPager.setCurrentItem(initialPosition, false)
        regionAdapter.setSelectedPosition(initialPosition)

        // Set the selected region based on initial position
        if (DataManager.usRegions.isNotEmpty() && initialPosition < DataManager.usRegions.size) {
            selectedRegion = DataManager.usRegions[initialPosition]
            selectedRegionName = selectedRegion?.name
            currentRegion = selectedRegion
            updatePageIndicator(initialPosition)
        }
    }

    private fun determineInitialRegionPosition(): Int {
        // First priority: If currently hunting, use hunting region
        if (isHunting) {
            val huntingRegion = prefs.getString("current_region", null)
            if (huntingRegion != null) {
                val index = DataManager.usRegions.indexOfFirst { it.name == huntingRegion }
                if (index >= 0) return index
            }
        }

        // Second priority: Use last selected region
        val lastRegion = prefs.getString("last_selected_region_United States", null)
        if (lastRegion != null) {
            val index = DataManager.usRegions.indexOfFirst { it.name == lastRegion }
            if (index >= 0) return index
        }

        // Default to first region
        return 0
    }

    private fun setupPageIndicator(pageCount: Int) {
        pageIndicator.removeAllViews()

        for (i in 0 until pageCount) {
            val dot = ImageView(context)
            dot.setImageResource(R.drawable.page_indicator_dot)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(4, 0, 4, 0)
            dot.layoutParams = params

            pageIndicator.addView(dot)
        }

        // Select first dot
        updatePageIndicator(0)
    }

    private fun updatePageIndicator(position: Int) {
        for (i in 0 until pageIndicator.childCount) {
            val dot = pageIndicator.getChildAt(i) as ImageView
            if (i == position) {
                dot.setImageResource(R.drawable.page_indicator_dot_selected)
            } else {
                dot.setImageResource(R.drawable.page_indicator_dot)
            }
        }
    }

    private fun selectCountry(index: Int) {
        if (index != 0) {
            Toast.makeText(context, "This country is coming soon!", Toast.LENGTH_SHORT).show()
            return
        }

        selectedCountryIndex = index
        selectedCountry = "United States"

        // Update UI to show selected country
        for (i in 0 until countryContainer.childCount) {
            val bubbleView = countryContainer.getChildAt(i)
            val card = bubbleView.findViewById<CardView>(R.id.country_card)
            card?.cardElevation = if (i == index) 8f else 2f
        }
    }

    private fun isRequestingPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasNotificationPermission() &&
                prefs.getBoolean("first_app_launch", true)
    }

    private fun initializeTutorial() {
        tutorialOverlay?.cleanup()
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        tutorialOverlay = TutorialOverlay(requireContext(), rootView)
    }

    private fun checkAndShowTutorial() {
        // Don't show tutorial without permission
        if (!hasActivityRecognitionPermission()) {
            return
        }

        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)

        if (!tutorialCompleted && !isHunting && isViewsReady()) {
            startTutorial()
        }
    }
    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed before Android Q
        }
    }

    private fun isViewsReady(): Boolean {
        return try {
            regionViewPager.adapter != null &&
                    startHuntButton != null &&
                    view != null
        } catch (e: Exception) {
            false
        }
    }

    private fun startTutorial() {
        if (hasPendingTutorial) return

        if (tutorialOverlay == null) {
            initializeTutorial()
        }

        hasPendingTutorial = true

        tutorialOverlay?.startTutorial {
            Toast.makeText(requireContext(), "Tutorial completed! Ready to hunt!", Toast.LENGTH_SHORT).show()
            hasPendingTutorial = false
            resetUIAfterTutorial()
        }
    }

    private fun resetUIAfterTutorial() {
        startHuntButton.isEnabled = true
        // Re-enable UI components after tutorial
        setupCountrySelector()
        setupRegionCards()
        updateUI()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        prefs.edit().putBoolean("first_app_launch", false).apply()

        // Check if permission was actually granted
        when (requestCode) {
            1001 -> { // ACTIVITY_RECOGNITION permission
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Skip all checks and start tutorial directly
                    val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)
                    if (!tutorialCompleted && !isHunting && !hasPendingTutorial) {
                        startTutorial()
                    }
                }
            }
            1002 -> { // POST_NOTIFICATIONS permission
                // Handle notification permission separately
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
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
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun checkForRegionChange() {
        if (isHunting && !hasCompletedCurrentHunt) {
            if ((selectedCountry != huntingCountry || selectedRegionName != huntingRegionName)
                && selectedCountry != null && selectedRegionName != null
            ) {
                startHuntButton.text = "Change Region?"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_orange_dark
                    )
                )
            } else {
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
// In HuntFragment.kt, update the recordDailyHunt method:

    private fun recordDailyHunt() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

        val streakPrefs = requireContext().getSharedPreferences("StreakData", Context.MODE_PRIVATE)
        val lastCompletedDate = streakPrefs.getString("last_hunt_date", "") ?: ""
        val currentCycleStart = streakPrefs.getString("cycle_start_date", today) ?: today
        val streakDays = streakPrefs.getStringSet("streak_days", mutableSetOf()) ?: mutableSetOf()

        // Check if streak is still consecutive
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

        // Get total consecutive days
        var totalConsecutiveDays = streakPrefs.getInt("total_consecutive_days", 0)

        // Get total lures earned from streaks (for tracking)
        var totalLuresFromStreaks = streakPrefs.getInt("total_lures_earned_from_streaks", 0)

        // Check if this is a new day
        if (lastCompletedDate != today) {
            // Check if we're continuing a streak or starting fresh
            if (lastCompletedDate == yesterday) {
                // Continuing streak
                totalConsecutiveDays++
            } else if (lastCompletedDate.isEmpty() || calculateDaysBetween(lastCompletedDate, today) > 1) {
                // Broken streak or first hunt - reset to 1
                totalConsecutiveDays = 1
            } else {
                // Same day, don't increment
            }

            val mutableStreakDays = streakDays.toMutableSet()

            // Check if we need to start a new reward cycle (after 7 days)
            val daysSinceCycleStart = calculateDaysBetween(currentCycleStart ?: today, today)

            if (daysSinceCycleStart >= 7) {
                // Start new reward cycle (but keep total consecutive days)
                mutableStreakDays.clear()
                streakPrefs.edit()
                    .putString("cycle_start_date", today)
                    .apply()
            }

            // Add today to streak days for reward tracking
            mutableStreakDays.add(today)

            // Check for lure rewards
            val streakCount = mutableStreakDays.size
            var luresAwarded = 0
            var message = ""

            when (streakCount) {
                2 -> {
                    luresAwarded = 1
                    message = "🎯 2-Day Streak! +1 Lure earned!"
                }
                4 -> {
                    luresAwarded = 1
                    message = "🎯 4-Day Streak! +1 Lure earned!"
                }
                7 -> {
                    luresAwarded = 2
                    message = "🔥 WEEKLY STREAK COMPLETE! +2 Lures earned!"
                }
            }

            // Add bonus message for milestone streaks
            when (totalConsecutiveDays) {
                14 -> message = "$message\n🌟 14-DAY STREAK MILESTONE!"
                30 -> message = "$message\n🏆 30-DAY STREAK ACHIEVEMENT!"
                50 -> message = "$message\n💎 50-DAY STREAK LEGEND!"
                100 -> message = "$message\n👑 100-DAY STREAK MASTER!"
            }

            if (luresAwarded > 0) {
                // Award lures
                repeat(luresAwarded) {
                    DataManager.addLure()
                }

                // Track total lures earned from streaks
                totalLuresFromStreaks += luresAwarded

                // IMPORTANT: Save the updated total immediately
                streakPrefs.edit()
                    .putInt("total_lures_earned_from_streaks", totalLuresFromStreaks)
                    .apply()

                // Show celebration dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Streak Reward!")
                    .setMessage(message)
                    .setPositiveButton("Awesome!") { dialog, _ ->
                        dialog.dismiss()
                        updateLureDisplay()
                    }
                    .setCancelable(false)
                    .show()
            }

            // Save updated streak data including total lures earned
            streakPrefs.edit()
                .putString("last_hunt_date", today)
                .putStringSet("streak_days", mutableStreakDays)
                .putInt("total_consecutive_days", totalConsecutiveDays)
                .putInt("total_lures_earned_from_streaks", totalLuresFromStreaks)
                .apply()

            // Update UI
            updateStreakDisplay()
        }
    }

    private fun updateStreakDisplay() {
        val streakPrefs = requireContext().getSharedPreferences("StreakData", Context.MODE_PRIVATE)
        val streakDays = streakPrefs.getStringSet("streak_days", setOf()) ?: setOf()
        val totalConsecutiveDays = streakPrefs.getInt("total_consecutive_days", 0)
        val lastHuntDate = streakPrefs.getString("last_hunt_date", "")

        // Check if streak is still active (was yesterday or today)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))

        val isStreakActive = lastHuntDate == today || lastHuntDate == yesterday

        // Update total streak display
        totalStreakText.text = when {
            totalConsecutiveDays == 0 || !isStreakActive -> "No active streak"
            totalConsecutiveDays == 1 -> "🔥 1 day streak"
            totalConsecutiveDays < 7 -> "🔥 $totalConsecutiveDays day streak"
            totalConsecutiveDays < 14 -> "🔥 $totalConsecutiveDays day streak!"
            totalConsecutiveDays < 30 -> "⭐ $totalConsecutiveDays day streak!"
            totalConsecutiveDays < 50 -> "🌟 $totalConsecutiveDays day streak!"
            totalConsecutiveDays < 100 -> "💎 $totalConsecutiveDays day streak!"
            else -> "👑 $totalConsecutiveDays day streak!"
        }

        // Color code based on streak length
        totalStreakText.setTextColor(when {
            !isStreakActive -> ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            totalConsecutiveDays < 7 -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            totalConsecutiveDays < 30 -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            totalConsecutiveDays < 100 -> ContextCompat.getColor(requireContext(), android.R.color.holo_purple)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        })

        // Clear existing bubbles
        streakBubblesContainer.removeAllViews()

        // Create 7 bubbles for the 7-day reward cycle
        for (day in 1..7) {
            val bubble = createStreakBubble(day, streakDays.size >= day)
            streakBubblesContainer.addView(bubble)

            if (day < 7) {
                val space = View(context)
                space.layoutParams = LinearLayout.LayoutParams(
                    8.dpToPx(),
                    1
                )
                streakBubblesContainer.addView(space)
            }
        }

        // Update streak label for reward cycle
        streakLabel.text = when (streakDays.size) {
            0 -> "Start earning rewards!"
            1 -> "Day 1 of 7 (Next reward: Day 2)"
            2 -> "Day 2 of 7 ✓"
            3 -> "Day 3 of 7 (Next reward: Day 4)"
            4 -> "Day 4 of 7 ✓"
            5 -> "Day 5 of 7"
            6 -> "Day 6 of 7 (Tomorrow: 2 lures!)"
            7 -> "Week complete! 🎉"
            else -> "Day ${streakDays.size} of 7"
        }
    }

    private fun createStreakBubble(dayNumber: Int, isCompleted: Boolean): View {
        val bubble = layoutInflater.inflate(R.layout.streak_bubble, null)
        val circleView = bubble.findViewById<View>(R.id.bubble_circle)
        val dayText = bubble.findViewById<TextView>(R.id.bubble_day_text)
        val rewardIcon = bubble.findViewById<ImageView>(R.id.bubble_reward_icon)

        dayText.text = dayNumber.toString()

        // Set background based on completion status
        if (isCompleted) {
            circleView.setBackgroundResource(R.drawable.streak_bubble_completed)
            dayText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            circleView.setBackgroundResource(R.drawable.streak_bubble_empty)
            dayText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        // Show reward icon for reward days
        when (dayNumber) {
            2, 4 -> {
                rewardIcon.visibility = View.VISIBLE
                rewardIcon.setImageResource(R.drawable.ic_lure_small)
                // Tint the icon if needed
                rewardIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            7 -> {
                rewardIcon.visibility = View.VISIBLE
                rewardIcon.setImageResource(R.drawable.ic_lure_double)
                // Tint the icon gold for double reward
                rewardIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            else -> {
                rewardIcon.visibility = View.GONE
            }
        }

        return bubble
    }

    private fun calculateDaysBetween(startDate: String, endDate: String): Int {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val start = format.parse(startDate)
            val end = format.parse(endDate)
            val diff = end.time - start.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun restoreHuntState() {
        isHunting = prefs.getBoolean("is_hunting", false)
        stepCount = prefs.getInt("current_steps", 0)
        initialStepCount = prefs.getInt("initial_step_count", -1)
        isUsingLure = prefs.getBoolean("using_lure", false)

        // Apply lure visual effects if lure is active
        if (isUsingLure && isHunting) {
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        } else {
            progressBar.progressTintList = null
        }

        if (isHunting) {
            val savedCountry = prefs.getString("current_country", "") ?: ""
            val savedRegion = prefs.getString("current_region", "") ?: ""

            if (savedCountry.isNotEmpty() && savedRegion.isNotEmpty()) {
                huntingCountry = savedCountry
                huntingRegionName = savedRegion
                selectedCountry = savedCountry
                selectedRegionName = savedRegion

                currentRegion = DataManager.usRegions.find { it.name == savedRegion }
                selectedRegion = currentRegion

                startHuntButton.text = "Stop Hunting"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
                )

                val huntCompleted = prefs.getBoolean("hunt_completed", false)
                val catchProcessed = prefs.getBoolean("catch_processed", false)

                // Set hunt status text based on current state
                huntStatusText.text = when {
                    huntCompleted && !catchProcessed -> "Goal reached! Opening your catch..."
                    huntCompleted && catchProcessed -> "Goal reached! Continue hunting or stop to reset."
                    isUsingLure -> "🎯 LURE ACTIVE!"
                    else -> ""
                }

                if (huntCompleted && catchProcessed) {
                    hasCompletedCurrentHunt = true
                } else if (!huntCompleted) {
                    stepSensor?.let {
                        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                    showNotification()
                }
            } else {
                isHunting = false
                prefs.edit().putBoolean("is_hunting", false).apply()

                startHuntButton.text = "Start Hunting"
                startHuntButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_green_dark
                    )
                )
                huntStatusText.text = ""
            }
        } else {
            startHuntButton.text = "Start Hunting"
            startHuntButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.holo_green_dark
                )
            )
            huntStatusText.text = ""
        }

        updateUI()
    }

    private fun startHunting() {
        if (selectedRegion == null || selectedRegionName == null) {
            Toast.makeText(context, "Please select a region", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCountry = "United States"
        val selectedRegionName = selectedRegionName ?: return

        // Check for lures and ask if they want to use one
        val lureCount = DataManager.getLureCount()
        if (lureCount > 0) {
            showLureDialog(selectedCountry, selectedRegionName)
        } else {
            startHuntingWithLure(selectedCountry, selectedRegionName, false)
        }
    }

    private fun showLureDialog(selectedCountry: String, selectedRegionName: String) {
        val lureCount = DataManager.getLureCount()

        AlertDialog.Builder(requireContext())
            .setTitle("Use a Lure?")
            .setMessage("You have $lureCount lure${if (lureCount > 1) "s" else ""}.\n\nUsing a lure greatly increases your chances of catching rare and legendary animals!\n\nWould you like to use one?")
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
        currentRegion = selectedRegion

        isHunting = true
        isUsingLure = useLure
        stepCount = 0
        initialStepCount = -1
        hasCompletedCurrentHunt = false
        isShowingDialog = false

        huntingCountry = selectedCountry
        huntingRegionName = selectedRegionName
        this.selectedCountry = selectedCountry
        this.selectedRegionName = selectedRegionName

        prefs.edit()
            .putBoolean("is_hunting", true)
            .putString("current_country", selectedCountry)
            .putString("current_region", selectedRegionName)
            .putString("last_selected_region_United States", selectedRegionName) // Save as last selected
            .putInt("current_steps", 0)
            .putInt("initial_step_count", -1)
            .putBoolean("hunt_completed", false)
            .putBoolean("catch_processed", false)
            .putBoolean("using_lure", useLure)
            .apply()

        startHuntButton.text = "Stop Hunting"
        startHuntButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_light
            )
        )
        // Removed: currentRegionText.text = "Hunting in: $selectedRegionName"

        huntStatusText.text = if (useLure) {
            "🎯 LURE ACTIVE!"
        } else {
            ""  // Clear the text when not using lure
        }

        if (useLure) {
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            )
        } else {
            progressBar.progressTintList = null
        }

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
                stopHunting()
                startHunting()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Reset to current hunting region
                val regionIndex = DataManager.usRegions.indexOfFirst { it.name == huntingRegionName }
                if (regionIndex >= 0) {
                    regionViewPager.setCurrentItem(regionIndex, true)
                    regionAdapter.setSelectedPosition(regionIndex)
                    selectedRegion = DataManager.usRegions[regionIndex]
                    selectedRegionName = huntingRegionName
                    currentRegion = selectedRegion
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
        huntStatusText.text = ""

        sensorManager?.unregisterListener(this)

        // Cancel the notification when stopping hunting
        cancelNotification()

        try {
            StepCounterService.stopService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateUI()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Add safety check for first launch edge case
        if (!prefs.getBoolean("app_initialized", false)) {
            return
        }

        if (isHunting && !hasCompletedCurrentHunt && event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
                prefs.edit().putInt("initial_step_count", initialStepCount).apply()
                stepCount = 0
                prefs.edit().putInt("current_steps", 0).apply()
                updateUI()
                return
            }

            val newStepCount = event.values[0].toInt() - initialStepCount

            // Protection against invalid values
            if (newStepCount < 0 || newStepCount > STEPS_REQUIRED * 10) {
                // Reset if we get an impossible value
                initialStepCount = event.values[0].toInt()
                stepCount = 0
                prefs.edit()
                    .putInt("initial_step_count", initialStepCount)
                    .putInt("current_steps", 0)
                    .apply()
                return
            }

            stepCount = newStepCount

            prefs.edit().putInt("current_steps", stepCount).apply()

            if (stepCount >= STEPS_REQUIRED && !hasCompletedCurrentHunt && !isShowingDialog) {
                catchAnimal()
            }

            updateUI()
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
            huntStatusText.text =
                ""
        }
    }

    private fun showNotification() {
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
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(STEPS_REQUIRED, currentSteps, false)
                .setSubText("$stepsRemaining steps to go • ${progress}% complete")
                .setOnlyAlertOnce(true)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
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
                .setAutoCancel(true)
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
        synchronized(catchLock) {
            val currentTime = System.currentTimeMillis()
            if (isCatchInProgress || (currentTime - lastCatchAttempt) < 2000) {
                return
            }

            if (hasCompletedCurrentHunt || isShowingDialog) {
                return
            }

            val catchProcessed = prefs.getBoolean("catch_processed", false)
            if (catchProcessed) {
                hasCompletedCurrentHunt = true
                return
            }

            // Set flags immediately
            isCatchInProgress = true
            lastCatchAttempt = currentTime
            hasCompletedCurrentHunt = true
            isShowingDialog = true

            // DON'T set catch_processed to true yet - wait until dialog is shown
            prefs.edit()
                .putBoolean("hunt_completed", true)
                // REMOVED: .putBoolean("catch_processed", true)
                .commit()
        }

        sensorManager?.unregisterListener(this)

        try {
            StepCounterService.stopService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        currentRegion?.let { region ->
            val caughtAnimal = if (isUsingLure) {
                selectRareAnimal(region.animals)
            } else {
                selectRandomAnimal(region.animals)
            }

            val isDuplicate = DataManager.addToCollection(caughtAnimal)
            DataManager.addExploredRegion(region.name)

            recordDailyHunt()

            val totalSteps = prefs.getInt("total_lifetime_steps", 0)
            prefs.edit()
                .putInt("total_lifetime_steps", totalSteps + STEPS_REQUIRED)
                .apply()

            // UPDATE THE REGION CARDS TO REFLECT NEW COLLECTION
            activity?.runOnUiThread {
                val updatedCollection = DataManager.getCollection().toSet()
                regionAdapter.updateCollection(updatedCollection)
                regionAdapter.notifyDataSetChanged()

                val currentPosition = regionViewPager.currentItem
                regionViewPager.adapter = regionAdapter
                regionViewPager.setCurrentItem(currentPosition, false)

                updateLureDisplay()
            }

            // NOW set catch_processed to true after successfully preparing the dialog
            prefs.edit().putBoolean("catch_processed", true).apply()

            val dialog = AnimalCaughtDialogWithLure(caughtAnimal, isDuplicate, isUsingLure) {
                activity?.runOnUiThread {
                    updateLureDisplay()
                    // Reset the flag when dialog is dismissed
                    isCatchInProgress = false
                }
                continueHunting()
            }
            dialog.show(childFragmentManager, "animal_caught")

            isUsingLure = false
            prefs.edit().putBoolean("using_lure", false).apply()
        } ?: run {
            // If currentRegion is null, reset the flag
            isCatchInProgress = false
        }
    }


    private fun selectRareAnimal(animals: List<Animal>): Animal {
        // Filter out COMMON animals - include Uncommon, Rare, and Legendary
        val nonCommonAnimals = animals.filter {
            it.rarity != Rarity.COMMON
        }

        // If no non-common animals available (shouldn't happen), fall back to normal selection
        if (nonCommonAnimals.isEmpty()) {
            return selectRandomAnimal(animals)
        }

        // Define custom weights for lure selection
        val lureWeights = mapOf(
            Rarity.UNCOMMON to 50,   // 50% chance
            Rarity.RARE to 35,        // 35% chance
            Rarity.LEGENDARY to 15    // 15% chance
        )

        // First, determine which rarity tier we're selecting with lure weights
        val totalWeight = lureWeights.values.sum()
        var random = Random.nextInt(totalWeight)
        var selectedRarity: Rarity? = null

        for ((rarity, weight) in lureWeights) {
            random -= weight
            if (random < 0) {
                selectedRarity = rarity
                break
            }
        }

        // Get animals of the selected rarity
        val animalsOfSelectedRarity = nonCommonAnimals.filter { it.rarity == selectedRarity }
        if (animalsOfSelectedRarity.isEmpty()) {
            return nonCommonAnimals.last() // Fallback
        }

        // Apply the 90% uncollected preference logic (same as in selectRandomAnimal)
        val collection = DataManager.getCollection()
        val uncollectedAnimals = animalsOfSelectedRarity.filter { animal ->
            !collection.contains(animal)
        }

        // If there are uncollected animals of this rarity
        return if (uncollectedAnimals.isNotEmpty()) {
            // 90% chance to get an uncollected animal
            if (Random.nextInt(100) < 90) {
                uncollectedAnimals.random()
            } else {
                // 10% chance to get any animal of this rarity
                animalsOfSelectedRarity.random()
            }
        } else {
            // All animals of this rarity collected - equal chance for all
            animalsOfSelectedRarity.random()
        }
    }

    private fun updateLureDisplay() {
        val lureCount = DataManager.getLureCount()
        lureCountText.text = "Lures: $lureCount"
        lureCountText.visibility = View.VISIBLE
        lureCountText.parent?.requestLayout()
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
            .putBoolean("catch_processed", false)
            .apply()

        if (isHunting) {
            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }

            // Check if lure is still active for the status text
            huntStatusText.text = if (isUsingLure) {
                "🎯 LURE ACTIVE!"
            } else {
                ""
            }

            try {
                StepCounterService.startService(requireContext())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        updateLureDisplay()
        updateUI()
    }

    private fun selectRandomAnimal(animals: List<Animal>): Animal {
        // Calculate total weight based on rarity weights, not individual animals
        val totalWeight = Rarity.values().sumOf { it.weight }  // This will always be 100
        var random = Random.nextInt(totalWeight)

        // First, determine which rarity tier we're selecting
        var selectedRarity: Rarity? = null
        var accumulatedWeight = 0

        for (rarity in Rarity.values()) {
            accumulatedWeight += rarity.weight
            if (random < accumulatedWeight) {
                selectedRarity = rarity
                break
            }
        }

        // Get animals of the selected rarity
        val animalsOfSelectedRarity = animals.filter { it.rarity == selectedRarity }
        if (animalsOfSelectedRarity.isEmpty()) {
            return animals.last() // Fallback if no animals of this rarity
        }

        // Check for uncollected animals of this rarity
        val collection = DataManager.getCollection()
        val uncollectedAnimals = animalsOfSelectedRarity.filter { animal ->
            !collection.contains(animal)
        }

        // If there are uncollected animals of this rarity
        return if (uncollectedAnimals.isNotEmpty()) {
            // 90% chance to get an uncollected animal
            if (Random.nextInt(100) < 90) {
                // Pick randomly from uncollected animals
                uncollectedAnimals.random()
            } else {
                // 10% chance to get any animal of this rarity (including duplicates)
                animalsOfSelectedRarity.random()
            }
        } else {
            // All animals of this rarity collected - equal chance for all
            animalsOfSelectedRarity.random()
        }
    }

    override fun onPause() {
        super.onPause()

        if (tutorialOverlay?.isActive() == true) {
            // Tutorial will handle saving its state
        }

        if (isHunting && !hasCompletedCurrentHunt) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tutorialOverlay?.cleanup()
        tutorialOverlay = null
    }

    override fun onResume() {
        super.onResume()

        updateLureDisplay()
        updateStreakDisplay()

        checkForTutorialOnResume()

        if (isHunting) {
            val latestSteps = prefs.getInt("current_steps", 0)
            stepCount = latestSteps

            // Check for pending catch again in case it wasn't handled in onViewCreated
            checkForPendingCatch()

            val huntCompleted = prefs.getBoolean("hunt_completed", false)
            if (!huntCompleted && !hasCompletedCurrentHunt) {
                stepSensor?.let {
                    sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
            }

            updateUI()
        }

        // Reset isShowingDialog only if no dialog is actually showing
        if (childFragmentManager.findFragmentByTag("animal_caught") == null) {
            isShowingDialog = false
        }
    }

    private fun checkForTutorialOnResume() {
        if (hasPendingTutorial) return

        // Check if we have the required permission first
        if (!hasActivityRecognitionPermission()) {
            return // Don't show tutorial without permission
        }

        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)
        val tutorialInProgress = prefs.getBoolean("tutorial_in_progress", false)

        if (!tutorialCompleted && !isHunting && isResumed && isViewsReady()) {
            if (tutorialInProgress) {
                showTutorialResumeDialog()
            } else {
                startTutorial()
            }
        }
    }

    private fun showTutorialResumeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Continue Tutorial?")
            .setMessage("You were in the middle of the tutorial. Would you like to continue where you left off or restart?")
            .setPositiveButton("Continue") { _, _ ->
                startTutorial()
            }
            .setNegativeButton("Restart") { _, _ ->
                prefs.edit()
                    .putBoolean("tutorial_in_progress", false)
                    .putInt("tutorial_current_step", 0)
                    .apply()
                startTutorial()
            }
            .setNeutralButton("Skip Tutorial") { _, _ ->
                prefs.edit()
                    .putBoolean("tutorial_completed", true)
                    .putBoolean("tutorial_in_progress", false)
                    .putInt("tutorial_current_step", 0)
                    .apply()
            }
            .setCancelable(false)
            .show()
    }
}