package com.example.stepcounterhunting

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    private var epicProgress: ProgressBar? = null
    private var epicCount: TextView? = null
    private var legendaryProgress: ProgressBar? = null
    private var legendaryCount: TextView? = null

    // Hunt stats views
    private var totalHuntsValue: TextView? = null
    private var avgStepsPerHunt: TextView? = null
    private var favoriteRegionValue: TextView? = null

    // Button
    private var replayTutorialButton: Button? = null

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

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
        epicProgress = view.findViewById(R.id.epic_progress)
        epicCount = view.findViewById(R.id.epic_count)
        legendaryProgress = view.findViewById(R.id.legendary_progress)
        legendaryCount = view.findViewById(R.id.legendary_count)

        // Hunt stats views
        totalHuntsValue = view.findViewById(R.id.total_hunts_value)
        avgStepsPerHunt = view.findViewById(R.id.avg_steps_per_hunt)
        favoriteRegionValue = view.findViewById(R.id.favorite_region_value)

        // Button
        replayTutorialButton = view.findViewById(R.id.replay_tutorial_button)
        replayTutorialButton?.setOnClickListener {
            showReplayTutorialDialog()
        }
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
    }

    private fun updateEnhancedStats(stats: UserStats) {
        val prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

        // Get additional data from DataManager
        val collection = DataManager.getCollection()
        val uniqueAnimals = collection.distinctBy { it.id }
        val currentLures = DataManager.getLureCount()
        val exploredRegions = DataManager.getExploredRegions()

        // Calculate total regions (5 US regions + other countries)
        val totalRegions = DataManager.usRegions.size + 20 // Rough estimate for other countries

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

        // Calculate lures earned total (duplicates found)
        val duplicatesFound = collection.size - uniqueAnimals.size
        luresEarnedTotal?.text = "$duplicatesFound earned total"

        // Update rarity distribution
        updateRarityDistribution(uniqueAnimals)

        // Update hunt statistics
        updateHuntStatistics(stats, collection, exploredRegions)
    }

    private fun updateRarityDistribution(uniqueAnimals: List<Animal>) {
        // Calculate rarity counts
        val rarityCount = mutableMapOf(
            Rarity.COMMON to 0,
            Rarity.UNCOMMON to 0,
            Rarity.RARE to 0,
            Rarity.EPIC to 0,
            Rarity.LEGENDARY to 0
        )

        uniqueAnimals.forEach { animal ->
            rarityCount[animal.rarity] = rarityCount[animal.rarity]!! + 1
        }

        val maxCount = rarityCount.values.maxOrNull() ?: 1

        // Update progress bars if they exist
        commonProgress?.let { updateRarityBar(it, commonCount, rarityCount[Rarity.COMMON] ?: 0, maxCount) }
        uncommonProgress?.let { updateRarityBar(it, uncommonCount, rarityCount[Rarity.UNCOMMON] ?: 0, maxCount) }
        rareProgress?.let { updateRarityBar(it, rareCount, rarityCount[Rarity.RARE] ?: 0, maxCount) }
        epicProgress?.let { updateRarityBar(it, epicCount, rarityCount[Rarity.EPIC] ?: 0, maxCount) }
        legendaryProgress?.let { updateRarityBar(it, legendaryCount, rarityCount[Rarity.LEGENDARY] ?: 0, maxCount) }
    }

    private fun updateRarityBar(progressBar: ProgressBar, countText: TextView?, count: Int, maxCount: Int) {
        val percentage = if (maxCount > 0) (count * 100) / maxCount else 0
        progressBar.progress = percentage
        countText?.text = count.toString()
    }

    private fun updateHuntStatistics(stats: UserStats, collection: List<Animal>, exploredRegions: Set<String>) {
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

    private fun showReplayTutorialDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Replay Tutorial")
            .setMessage("Would you like to replay the hunting tutorial?")
            .setPositiveButton("Yes") { _, _ ->
                restartTutorialAndNavigate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restartTutorialAndNavigate() {
        // Clear the tutorial completed flag
        val prefs = requireContext().getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("tutorial_completed", false)
            .putBoolean("tutorial_in_progress", false)
            .putInt("tutorial_current_step", 0)
            .apply()

        // Navigate to HuntFragment
        // Adjust this based on your MainActivity navigation implementation
        Toast.makeText(requireContext(), "Tutorial will start when you open the Hunt tab", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}

