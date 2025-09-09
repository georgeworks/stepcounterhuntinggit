package com.example.stepcounterhunting

import android.app.AlertDialog
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.widget.Switch
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ClipData

class ProfileFragment : Fragment() {

    // Basic views (for backward compatibility with old layout)
    private lateinit var totalStepsText: TextView
    private lateinit var animalsCollectedText: TextView
    private lateinit var regionsExploredText: TextView

    // Enhanced views (optional - will be null if using old layout)
    private var totalStepsValue: TextView? = null
    private var dailyAverageSteps: TextView? = null
    private var animalsCollectedValue: TextView? = null
    private var uniqueAnimalsText: TextView? = null
    private var regionsExploredValue: TextView? = null
    private var regionsTotal: TextView? = null
    private var luresValue: TextView? = null
    private var luresEarnedTotal: TextView? = null

    // Rarity distribution views
    private var commonProgress: ProgressBar? = null
    private var commonCount: TextView? = null
    private var uncommonProgress: ProgressBar? = null
    private var uncommonCount: TextView? = null
    private var rareProgress: ProgressBar? = null
    private var rareCount: TextView? = null
    private var legendaryProgress: ProgressBar? = null
    private var legendaryCount: TextView? = null

    // Hunt stats views
    private var totalHuntsValue: TextView? = null
    private var avgStepsPerHunt: TextView? = null
    private var favoriteRegionValue: TextView? = null

    // Stamp grid views
    private var regionsGrid: GridLayout? = null
    private var countriesGrid: GridLayout? = null
    private var challengesGrid: GridLayout? = null

    // ButtoN
    private var notificationsSwitch: Switch? = null
    private var sendFeedbackText: TextView? = null
    private var privacyPolicyText: TextView? = null
    private var versionText: TextView? = null
    private var buildText: TextView? = null
    private var resetProgressButton: Button? = null

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    // Challenge definitions
    data class Challenge(
        val id: String,
        val name: String,
        val description: String,
        val iconResource: Int,
        val checkCompletion: (ProfileFragment) -> Boolean
    )

    private val challenges = listOf(
        Challenge(
            "duplicate_hunter",
            "Duplicate Hunter",
            "Catch 20 duplicate animals",
            android.R.drawable.ic_menu_rotate
        ) { fragment ->
            val collection = DataManager.getCollection()
            val uniqueAnimals = collection.distinctBy { it.id }
            val duplicates = collection.size - uniqueAnimals.size
            duplicates >= 20
        },

        Challenge(
            "legendary_seeker",
            "Legendary Seeker",
            "Catch 5 legendary animals",
            android.R.drawable.star_big_on
        ) { fragment ->
            val collection = DataManager.getCollection()
            collection.count { it.rarity == Rarity.LEGENDARY } >= 5
        },

        Challenge(
            "step_master",
            "Step Master",
            "Hunt for 25,000 total steps",
            android.R.drawable.ic_menu_directions
        ) { fragment ->
            val stats = DataManager.getStats()
            stats.totalSteps >= 25000
        },

        Challenge(
            "region_explorer",
            "Region Explorer",
            "Explore 3 different regions",
            android.R.drawable.ic_menu_mapmode
        ) { fragment ->
            val stats = DataManager.getStats()
            stats.regionsExplored >= 3
        },

        Challenge(
            "collector",
            "Collector",
            "Catch 25 animals total",
            android.R.drawable.ic_menu_gallery
        ) { fragment ->
            val collection = DataManager.getCollection()
            collection.size >= 25
        },

        Challenge(
            "rare_finder",
            "Rare Finder",
            "Catch your first rare animal",
            android.R.drawable.ic_menu_search
        ) { fragment ->
            val collection = DataManager.getCollection()
            collection.any { it.rarity == Rarity.RARE || it.rarity == Rarity.LEGENDARY }
        },

        Challenge(
            "completionist",
            "Completionist",
            "Complete any region (catch all animals)",
            android.R.drawable.ic_menu_agenda
        ) { fragment ->
            val collection = DataManager.getCollection()
            val uniqueAnimals = collection.distinctBy { it.id }

            DataManager.usRegions.any { region ->
                region.animals.all { regionAnimal ->
                    uniqueAnimals.any { it.id == regionAnimal.id }
                }
            }
        },

        Challenge(
            "lure_master",
            "Lure Master",
            "Accumulate 15 lures",
            android.R.drawable.ic_menu_preferences
        ) { fragment ->
            val currentLures = DataManager.getLureCount()
            val collection = DataManager.getCollection()
            val uniqueAnimals = collection.distinctBy { it.id }
            val totalLuresEarned = collection.size - uniqueAnimals.size
            (currentLures + totalLuresEarned) >= 15
        },

        Challenge(
            "marathon_walker",
            "Marathon Walker",
            "Hunt for 50,000 total steps",
            android.R.drawable.ic_menu_compass
        ) { fragment ->
            val stats = DataManager.getStats()
            stats.totalSteps >= 50000
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find basic views (these should exist in both old and new layouts)
        totalStepsText = view.findViewById(R.id.total_steps_text)
        animalsCollectedText = view.findViewById(R.id.animals_collected_text)
        regionsExploredText = view.findViewById(R.id.regions_explored_text)

        // Try to find enhanced views (they may not exist if using old layout)
        initializeEnhancedViews(view)

        updateStats()
    }

    private fun initializeEnhancedViews(view: View) {
        // Main stat views
        totalStepsValue = view.findViewById(R.id.total_steps_value)
        dailyAverageSteps = view.findViewById(R.id.daily_average_steps)
        animalsCollectedValue = view.findViewById(R.id.animals_collected_value)
        uniqueAnimalsText = view.findViewById(R.id.unique_animals_text)
        regionsExploredValue = view.findViewById(R.id.regions_explored_value)
        regionsTotal = view.findViewById(R.id.regions_total)
        luresValue = view.findViewById(R.id.lures_value)
        luresEarnedTotal = view.findViewById(R.id.lures_earned_total)

        // Rarity distribution views
        commonProgress = view.findViewById(R.id.common_progress)
        commonCount = view.findViewById(R.id.common_count)
        uncommonProgress = view.findViewById(R.id.uncommon_progress)
        uncommonCount = view.findViewById(R.id.uncommon_count)
        rareProgress = view.findViewById(R.id.rare_progress)
        rareCount = view.findViewById(R.id.rare_count)
        legendaryProgress = view.findViewById(R.id.legendary_progress)
        legendaryCount = view.findViewById(R.id.legendary_count)

        // Hunt stats views
        totalHuntsValue = view.findViewById(R.id.total_hunts_value)
        avgStepsPerHunt = view.findViewById(R.id.avg_steps_per_hunt)
        favoriteRegionValue = view.findViewById(R.id.favorite_region_value)

        // Stamp grids
        regionsGrid = view.findViewById(R.id.regions_grid)
        countriesGrid = view.findViewById(R.id.countries_grid)
        challengesGrid = view.findViewById(R.id.challenges_grid)

        resetProgressButton = view.findViewById(R.id.reset_progress_button)


        notificationsSwitch = view.findViewById(R.id.notifications_switch)
        sendFeedbackText = view.findViewById(R.id.send_feedback_text)
        privacyPolicyText = view.findViewById(R.id.privacy_policy_text)
        versionText = view.findViewById(R.id.version_text)
        buildText = view.findViewById(R.id.build_text)

        initializeSettings()
    }
    private fun initializeSettings() {
        val prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)


        // Notifications
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        notificationsSwitch?.isChecked = notificationsEnabled
        notificationsSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(context,
                if (isChecked) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT).show()
        }

        // Send Feedback
        sendFeedbackText?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:admin@EastLumenz.com")
                putExtra(Intent.EXTRA_SUBJECT, "Step Hunter Feedback")
                putExtra(Intent.EXTRA_TEXT, "App Version: 0.1.0\nBuild: 1\n\nFeedback:\n")
            }

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Fallback: show dialog with email address they can copy
                AlertDialog.Builder(requireContext())
                    .setTitle("Send Feedback")
                    .setMessage("No email app found.\n\nPlease send feedback to:\nyour-email@example.com")
                    .setPositiveButton("Copy Email") { _, _ ->
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("email", "your-email@example.com")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("OK", null)
                    .show()
            }
        }

        // Privacy Policy
        privacyPolicyText?.setOnClickListener {
            // For now, show a dialog. Later you can open a web URL
            AlertDialog.Builder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage("Step Hunter collects minimal data:\n\n" +
                        "• Step count data stays on your device\n" +
                        "• No personal information is shared\n" +
                        "• No ads or tracking\n\n" +
                        "Full policy coming soon.")
                .setPositiveButton("OK", null)
                .show()
        }

        // Version info
        versionText?.text = "Version 0.5.0 (Beta)"
        buildText?.text = "Build 1"
    }
    private fun showResetProgressDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Progress?")
            .setMessage("This will delete ALL your data:\n• Animals caught\n• Steps counted\n• Lures earned\n• Streak progress\n\nThis cannot be undone!")
            .setPositiveButton("Reset") { _, _ ->
                // Clear all SharedPreferences
                context?.getSharedPreferences("StepCounter", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
                context?.getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
                context?.getSharedPreferences("StreakData", Context.MODE_PRIVATE)?.edit()?.clear()?.apply()

                // Reinitialize DataManager
                DataManager.init(requireContext())

                // Restart app
                activity?.recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateStats() {
        // Get data from DataManager and SharedPreferences
        val prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        val stats = DataManager.getStats()

        // Update basic views with stats from DataManager
        totalStepsText.text = "Total Steps: ${stats.totalSteps}"
        animalsCollectedText.text = "Animals Collected: ${stats.animalsCollected}"
        regionsExploredText.text = "Regions Explored: ${stats.regionsExplored}"

        // If we have the enhanced layout, update those views too
        if (totalStepsValue != null) {
            updateEnhancedStats(stats)
        }
        // Update stamp grids if they exist
        if (regionsGrid != null || countriesGrid != null) {
            updateCompletionStamps()
        }
        // Update challenge stamps if they exist
        if (challengesGrid != null) {
            updateChallengeStamps()
        }
    }

// In ProfileFragment.kt, update the updateEnhancedStats function:

    private fun updateEnhancedStats(stats: UserStats) {
        val prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        val streakPrefs = requireContext().getSharedPreferences("StreakData", Context.MODE_PRIVATE)

        // Get additional data from DataManager
        val collection = DataManager.getCollection()
        val uniqueAnimals = collection.distinctBy { it.id }
        val currentLures = DataManager.getLureCount()
        val exploredRegions = DataManager.getExploredRegions()

        // Calculate total regions
        val totalRegions = DataManager.usRegions.size

        // Update main stat cards
        totalStepsValue?.text = numberFormat.format(stats.totalSteps)
        animalsCollectedValue?.text = stats.animalsCollected.toString()
        uniqueAnimalsText?.text = "${uniqueAnimals.size} unique"
        regionsExploredValue?.text = stats.regionsExplored.toString()
        regionsTotal?.text = "of $totalRegions total"
        luresValue?.text = currentLures.toString()

        // Calculate and display daily average
        val firstLaunchTime = prefs.getLong("first_launch_timestamp", System.currentTimeMillis())
        if (!prefs.contains("first_launch_timestamp")) {
            prefs.edit().putLong("first_launch_timestamp", System.currentTimeMillis()).apply()
        }

        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - firstLaunchTime
        ).coerceAtLeast(1)
        val dailyAvg = stats.totalSteps / daysSinceFirstLaunch.toInt()
        dailyAverageSteps?.text = "${numberFormat.format(dailyAvg)} daily avg"

        // Calculate total lures earned from ALL sources

        // 1. Lures from duplicates
        val duplicatesFound = collection.size - uniqueAnimals.size

        // 2. Lures from streaks (tracked in SharedPreferences)
        val luresFromStreaks = streakPrefs.getInt("total_lures_earned_from_streaks", 0)

        // Total earned = duplicates + streak rewards
        val totalLuresEarned = duplicatesFound + luresFromStreaks
        luresEarnedTotal?.text = "$totalLuresEarned earned total"

        // Update rarity distribution
        updateRarityDistribution(collection)

        // Update hunt statistics
        updateHuntStatistics(stats, collection, exploredRegions)
    }

    private fun updateRarityDistribution(allCatches: List<Animal>) {  // Renamed parameter for clarity
        // Calculate rarity counts INCLUDING DUPLICATES
        val rarityCount = mutableMapOf(
            Rarity.COMMON to 0,
            Rarity.UNCOMMON to 0,
            Rarity.RARE to 0,
            Rarity.LEGENDARY to 0
        )

        // Count ALL catches, including duplicates
        allCatches.forEach { animal ->
            rarityCount[animal.rarity] = rarityCount[animal.rarity]!! + 1
        }

        val maxCount = rarityCount.values.maxOrNull() ?: 1

        // Update progress bars if they exist
        commonProgress?.let {
            updateRarityBar(
                it,
                commonCount,
                rarityCount[Rarity.COMMON] ?: 0,
                maxCount
            )
        }
        uncommonProgress?.let {
            updateRarityBar(
                it,
                uncommonCount,
                rarityCount[Rarity.UNCOMMON] ?: 0,
                maxCount
            )
        }
        rareProgress?.let {
            updateRarityBar(
                it,
                rareCount,
                rarityCount[Rarity.RARE] ?: 0,
                maxCount
            )
        }
        legendaryProgress?.let {
            updateRarityBar(
                it,
                legendaryCount,
                rarityCount[Rarity.LEGENDARY] ?: 0,
                maxCount
            )
        }
    }

    private fun updateRarityBar(
        progressBar: ProgressBar,
        countText: TextView?,
        count: Int,
        maxCount: Int
    ) {
        val percentage = if (maxCount > 0) (count * 100) / maxCount else 0
        progressBar.progress = percentage
        countText?.text = count.toString()
    }

    private fun updateHuntStatistics(
        stats: UserStats,
        collection: List<Animal>,
        exploredRegions: Set<String>
    ) {
        // Calculate total hunts (100 steps per hunt)
        val totalHunts = stats.totalSteps / 100
        totalHuntsValue?.text = totalHunts.toString()

        // Average steps should always be 100 unless there's an incomplete hunt
        val avgSteps = if (totalHunts > 0) stats.totalSteps / totalHunts else 100
        avgStepsPerHunt?.text = avgSteps.toString()

        // Find favorite region (most visited)
        if (exploredRegions.isNotEmpty()) {
            // Count animals per region to determine favorite
            val regionCounts = mutableMapOf<String, Int>()
            collection.forEach { animal ->
                regionCounts[animal.region] = regionCounts.getOrDefault(animal.region, 0) + 1
            }

            val favoriteRegion = regionCounts.maxByOrNull { it.value }?.key
            favoriteRegionValue?.text = favoriteRegion ?: exploredRegions.first()
        } else {
            favoriteRegionValue?.text = "None yet"
        }
    }

    private fun updateCompletionStamps() {
        val collection = DataManager.getCollection()
        val uniqueAnimals = collection.distinctBy { it.id }

        // Update US region stamps
        regionsGrid?.let { grid ->
            grid.removeAllViews()

            DataManager.usRegions.forEachIndexed { index, region ->
                val stampView = createStampView(region.name, uniqueAnimals, region.animals)
                // ADD PROPER LAYOUT PARAMS HERE
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(index % 3, 1f) // 3 columns with weight
                    rowSpec = GridLayout.spec(index / 3)
                    setMargins(8, 8, 8, 8)
                }
                stampView.layoutParams = params
                grid.addView(stampView)
            }
        }

        // Update country stamps
        countriesGrid?.let { grid ->
            grid.removeAllViews()

            val countries = listOf(
                CountryData("United States", DataManager.usRegions.flatMap { it.animals })
            )

            countries.forEachIndexed { index, country ->
                val stampView = createCountryStampView(country.name, uniqueAnimals, country.animals)
                // ADD PROPER LAYOUT PARAMS HERE
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(index % 3, 1f)
                    rowSpec = GridLayout.spec(index / 3)
                    setMargins(8, 8, 8, 8)
                }
                stampView.layoutParams = params
                grid.addView(stampView)
            }

            // Add "More countries coming soon" text with proper params
            val comingSoonText = TextView(context).apply {
                text = "More countries coming soon!"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                setPadding(16, 8, 16, 8)
            }
            val textParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(0, 3, 1f) // Span 3 columns
                rowSpec = GridLayout.spec(1)
                setMargins(8, 8, 8, 8)
            }
            comingSoonText.layoutParams = textParams
            grid.addView(comingSoonText)
        }
    }

    private fun updateChallengeStamps() {
        challengesGrid?.let { grid ->
            grid.removeAllViews()

            challenges.forEachIndexed { index, challenge ->
                val stampView = createChallengeStampView(challenge)
                // ADD PROPER LAYOUT PARAMS HERE
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(index % 3, 1f) // 3 columns with weight
                    rowSpec = GridLayout.spec(index / 3)
                    setMargins(8, 8, 8, 8)
                }
                stampView.layoutParams = params
                grid.addView(stampView)
            }
        }
    }

    private fun createChallengeStampView(challenge: Challenge): View {
        val inflater = LayoutInflater.from(context)
        val stampView = inflater.inflate(R.layout.stamp_item, null)

        val icon = stampView.findViewById<ImageView>(R.id.stamp_icon)
        val name = stampView.findViewById<TextView>(R.id.stamp_name)
        val progress = stampView.findViewById<TextView>(R.id.stamp_progress)

        // Check if challenge is completed
        val isComplete = challenge.checkCompletion(this)

        // Set name (break long names)
        name.text = challenge.name.replace(" ", "\n")

        // Set progress text
        progress.text = if (isComplete) "✓" else ""

        // Set icon and colors based on completion
        setStampAppearance(icon, isComplete, challenge.iconResource)

        // Add click listener to show challenge description
        stampView.setOnClickListener {
            showChallengeDialog(challenge, isComplete)
        }

        return stampView
    }

    private fun showChallengeDialog(challenge: Challenge, isComplete: Boolean) {
        val title = if (isComplete) "✓ ${challenge.name}" else challenge.name
        val message = if (isComplete) {
            challenge.description + "\n\nCompleted!"
        } else {
            challenge.description
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun createStampView(
        regionName: String,
        collectedAnimals: List<Animal>,
        regionAnimals: List<Animal>
    ): View {
        val inflater = LayoutInflater.from(context)
        val stampView = inflater.inflate(R.layout.stamp_item, null)

        val icon = stampView.findViewById<ImageView>(R.id.stamp_icon)
        val name = stampView.findViewById<TextView>(R.id.stamp_name)
        val progress = stampView.findViewById<TextView>(R.id.stamp_progress)

        // Count how many animals from this region the player has caught
        val caughtInRegion = collectedAnimals.count { caughtAnimal ->
            regionAnimals.any { it.id == caughtAnimal.id }
        }
        val totalInRegion = regionAnimals.size
        val isComplete = caughtInRegion == totalInRegion

        // Set name
        name.text = regionName.replace(" ", "\n") // Break long names

        // Set progress
        progress.text = "$caughtInRegion/$totalInRegion"

        // Set icon and colors based on completion
        setStampAppearance(icon, isComplete, getRegionIcon(regionName))

        return stampView
    }

    private fun createCountryStampView(countryName: String, collectedAnimals: List<Animal>, countryAnimals: List<Animal>): View {
        val inflater = LayoutInflater.from(context)
        val stampView = inflater.inflate(R.layout.stamp_item, null)

        val icon = stampView.findViewById<ImageView>(R.id.stamp_icon)
        val name = stampView.findViewById<TextView>(R.id.stamp_name)
        val progress = stampView.findViewById<TextView>(R.id.stamp_progress)

        // Count collected animals for this country
        val caughtInCountry = collectedAnimals.count { animal ->
            if (countryName == "United States") {
                DataManager.usRegions.any { region ->
                    region.animals.any { it.id == animal.id }
                }
            } else {
                animal.region.contains(countryName)
            }
        }

        val totalInCountry = if (countryName == "United States") {
            DataManager.usRegions.flatMap { it.animals }.size
        } else {
            5 // Default animals per country
        }

        val isComplete = caughtInCountry == totalInCountry

        // Set name
        name.text = countryName.replace(" ", "\n")

        // Set progress
        progress.text = "$caughtInCountry/$totalInCountry"

        // Set icon and colors - only show as complete when ALL animals are caught
        setStampAppearance(icon, isComplete, getCountryIcon(countryName))

        return stampView
    }

    private fun setStampAppearance(icon: ImageView, isComplete: Boolean, iconResource: Int) {
        icon.setImageResource(iconResource)

        if (!isComplete) {
            // Make it grayscale/silhouette
            val matrix = ColorMatrix()
            matrix.setSaturation(0f) // Remove color
            val filter = ColorMatrixColorFilter(matrix)
            icon.colorFilter = filter
            icon.alpha = 0.3f
        } else {
            // Full color for completed
            icon.colorFilter = null
            icon.alpha = 1.0f
        }
    }

    private fun getRegionIcon(regionName: String): Int {
        return when (regionName) {
            "The Appalachians" -> android.R.drawable.ic_menu_mapmode
            "Desert Southwest" -> android.R.drawable.ic_menu_compass
            "Pacific Northwest" -> android.R.drawable.ic_menu_gallery
            "Great Plains" -> android.R.drawable.ic_menu_view
            "Far North" -> android.R.drawable.ic_menu_myplaces
            else -> android.R.drawable.ic_menu_mapmode
        }
    }

    private fun getCountryIcon(countryName: String): Int {
        return when (countryName) {
            "United States" -> android.R.drawable.star_big_on
            "China" -> android.R.drawable.ic_menu_compass
            "Australia" -> android.R.drawable.ic_menu_mylocation
            "Brazil" -> android.R.drawable.ic_menu_gallery
            "Madagascar" -> android.R.drawable.ic_menu_info_details
            else -> android.R.drawable.ic_menu_mapmode
        }
    }

    private fun getCountryAnimals(countryName: String): List<Animal> {
        // For non-US countries, we're using the default animals
        // In a real app, you'd have specific animals for each country
        return DataManager.getDefaultAnimals()
    }

    data class CountryData(val name: String, val animals: List<Animal>)

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}