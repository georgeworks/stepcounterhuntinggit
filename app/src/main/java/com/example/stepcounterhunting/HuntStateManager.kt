package com.example.stepcounterhunting

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HuntStateManager {
    private const val PREFS_NAME = "hunt_state_prefs"
    private const val KEY_IS_HUNTING = "is_hunting"
    private const val KEY_STEP_COUNT = "step_count"
    private const val KEY_INITIAL_STEP_COUNT = "initial_step_count"
    private const val KEY_CURRENT_REGION = "current_region"
    private const val KEY_CURRENT_COUNTRY = "current_country"
    private const val KEY_TOTAL_STEPS = "total_steps"
    private const val KEY_COLLECTION = "collection"
    private const val KEY_EXPLORED_REGIONS = "explored_regions"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun startHunt(context: Context, country: String, region: Region) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_HUNTING, true)
            putString(KEY_CURRENT_COUNTRY, country)
            putString(KEY_CURRENT_REGION, Gson().toJson(region))
            putInt(KEY_STEP_COUNT, 0)
            putInt(KEY_INITIAL_STEP_COUNT, -1)
            apply()
        }
    }

    fun stopHunt(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_HUNTING, false)
            putInt(KEY_STEP_COUNT, 0)
            putInt(KEY_INITIAL_STEP_COUNT, -1)
            apply()
        }
    }

    fun updateStepCount(context: Context, steps: Int) {
        getPrefs(context).edit().apply {
            putInt(KEY_STEP_COUNT, steps)
            apply()
        }
        // Also update total steps
        val currentTotal = getPrefs(context).getInt(KEY_TOTAL_STEPS, 0)
        getPrefs(context).edit().putInt(KEY_TOTAL_STEPS, currentTotal + 1).apply()
    }

    fun updateInitialStepCount(context: Context, initialCount: Int) {
        getPrefs(context).edit().apply {
            putInt(KEY_INITIAL_STEP_COUNT, initialCount)
            apply()
        }
    }

    fun resetStepsAfterCatch(context: Context) {
        getPrefs(context).edit().apply {
            putInt(KEY_STEP_COUNT, 0)
            putInt(KEY_INITIAL_STEP_COUNT, -1)
            apply()
        }
    }

    fun getHuntState(context: Context): HuntState {
        val prefs = getPrefs(context)
        val regionJson = prefs.getString(KEY_CURRENT_REGION, null)
        val region = if (regionJson != null) {
            Gson().fromJson(regionJson, Region::class.java)
        } else null

        return HuntState(
            isHunting = prefs.getBoolean(KEY_IS_HUNTING, false),
            stepCount = prefs.getInt(KEY_STEP_COUNT, 0),
            initialStepCount = prefs.getInt(KEY_INITIAL_STEP_COUNT, -1),
            currentCountry = prefs.getString(KEY_CURRENT_COUNTRY, "") ?: "",
            currentRegion = region
        )
    }

    fun saveCollection(context: Context, collection: List<Animal>) {
        val json = Gson().toJson(collection)
        getPrefs(context).edit().putString(KEY_COLLECTION, json).apply()
    }

    fun loadCollection(context: Context): List<Animal> {
        val json = getPrefs(context).getString(KEY_COLLECTION, null) ?: return emptyList()
        val type = object : TypeToken<List<Animal>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveExploredRegions(context: Context, regions: Set<String>) {
        val json = Gson().toJson(regions)
        getPrefs(context).edit().putString(KEY_EXPLORED_REGIONS, json).apply()
    }

    fun loadExploredRegions(context: Context): Set<String> {
        val json = getPrefs(context).getString(KEY_EXPLORED_REGIONS, null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getTotalSteps(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_STEPS, 0)
    }

    data class HuntState(
        val isHunting: Boolean,
        val stepCount: Int,
        val initialStepCount: Int,
        val currentCountry: String,
        val currentRegion: Region?
    )
}
