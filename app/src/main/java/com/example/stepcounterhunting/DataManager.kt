package com.example.stepcounterhunting

import android.content.Context
import android.content.SharedPreferences

object DataManager {
    private lateinit var prefs: SharedPreferences
    private lateinit var stepPrefs: SharedPreferences  // Add this
    private val collection = mutableListOf<Animal>()
    private val exploredRegions = mutableSetOf<String>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)
        stepPrefs = context.getSharedPreferences("StepCounter", Context.MODE_PRIVATE)  // Initialize this
        loadData()
    }

    private fun loadData() {
        // Load collection (simplified - just stores animal IDs)
        val collectionString = prefs.getString("collection", "") ?: ""
        if (collectionString.isNotEmpty()) {
            val animalIds = collectionString.split(",")
            collection.clear()
            animalIds.forEach { id ->
                findAnimalById(id)?.let { collection.add(it) }
            }
        }

        // Load explored regions
        val regionsString = prefs.getString("explored_regions", "") ?: ""
        if (regionsString.isNotEmpty()) {
            exploredRegions.clear()
            exploredRegions.addAll(regionsString.split(","))
        }
    }

    private fun saveData() {
        // Save collection as comma-separated IDs
        val collectionString = collection.joinToString(",") { it.id }
        prefs.edit().putString("collection", collectionString).apply()

        // Save explored regions
        val regionsString = exploredRegions.joinToString(",")
        prefs.edit().putString("explored_regions", regionsString).apply()
    }

    private fun findAnimalById(id: String): Animal? {
        val allAnimals = usRegions.flatMap { it.animals } + getDefaultAnimals()
        return allAnimals.find { it.id == id }
    }

    val usRegions = listOf(
        Region(
            "Northeast",
            listOf(
                Animal("ne_1", "White-tailed Deer", "Common forest dweller", Rarity.COMMON, "Northeast"),
                Animal("ne_2", "Black Bear", "Large omnivore", Rarity.UNCOMMON, "Northeast"),
                Animal("ne_3", "Wild Turkey", "Native game bird", Rarity.COMMON, "Northeast"),
                Animal("ne_4", "Bobcat", "Elusive predator", Rarity.RARE, "Northeast"),
                Animal("ne_5", "Moose", "Largest deer species", Rarity.EPIC, "Northeast"),
                Animal("ne_6", "Eastern Cougar", "Mythical big cat", Rarity.LEGENDARY, "Northeast"),
                Animal("ne_7", "Red Fox", "Cunning canine", Rarity.COMMON, "Northeast"),
                Animal("ne_8", "River Otter", "Playful swimmer", Rarity.UNCOMMON, "Northeast"),
                Animal("ne_9", "Bald Eagle", "National bird", Rarity.RARE, "Northeast"),
                Animal("ne_10", "Gray Wolf", "Pack hunter", Rarity.LEGENDARY, "Northeast")
            )
        ),
        Region(
            "Southeast",
            listOf(
                Animal("se_1", "Alligator", "Swamp predator", Rarity.UNCOMMON, "Southeast"),
                Animal("se_2", "Armadillo", "Armored mammal", Rarity.COMMON, "Southeast"),
                Animal("se_3", "Wild Boar", "Invasive species", Rarity.COMMON, "Southeast"),
                Animal("se_4", "Florida Panther", "Endangered cat", Rarity.LEGENDARY, "Southeast"),
                Animal("se_5", "Manatee", "Gentle giant", Rarity.EPIC, "Southeast"),
                Animal("se_6", "Cottonmouth", "Venomous snake", Rarity.UNCOMMON, "Southeast"),
                Animal("se_7", "Raccoon", "Masked bandit", Rarity.COMMON, "Southeast"),
                Animal("se_8", "Great Blue Heron", "Tall wader", Rarity.UNCOMMON, "Southeast"),
                Animal("se_9", "Black Bear", "Forest dweller", Rarity.RARE, "Southeast"),
                Animal("se_10", "Red Wolf", "Critically endangered", Rarity.LEGENDARY, "Southeast")
            )
        ),
        Region(
            "Midwest",
            listOf(
                Animal("mw_1", "Bison", "Prairie giant", Rarity.EPIC, "Midwest"),
                Animal("mw_2", "Prairie Dog", "Social rodent", Rarity.COMMON, "Midwest"),
                Animal("mw_3", "Coyote", "Adaptable predator", Rarity.COMMON, "Midwest"),
                Animal("mw_4", "Badger", "Fierce digger", Rarity.UNCOMMON, "Midwest"),
                Animal("mw_5", "Elk", "Large ungulate", Rarity.RARE, "Midwest"),
                Animal("mw_6", "Golden Eagle", "Powerful raptor", Rarity.RARE, "Midwest"),
                Animal("mw_7", "White-tailed Deer", "Common herbivore", Rarity.COMMON, "Midwest"),
                Animal("mw_8", "River Otter", "Aquatic mammal", Rarity.UNCOMMON, "Midwest"),
                Animal("mw_9", "Sandhill Crane", "Tall migrant", Rarity.RARE, "Midwest"),
                Animal("mw_10", "Mountain Lion", "Apex predator", Rarity.LEGENDARY, "Midwest")
            )
        ),
        Region(
            "West",
            listOf(
                Animal("w_1", "Grizzly Bear", "Massive predator", Rarity.LEGENDARY, "West"),
                Animal("w_2", "Mountain Goat", "Cliff climber", Rarity.RARE, "West"),
                Animal("w_3", "Mule Deer", "Large-eared deer", Rarity.COMMON, "West"),
                Animal("w_4", "Bighorn Sheep", "Horned climber", Rarity.UNCOMMON, "West"),
                Animal("w_5", "California Condor", "Giant bird", Rarity.LEGENDARY, "West"),
                Animal("w_6", "Gray Wolf", "Pack hunter", Rarity.EPIC, "West"),
                Animal("w_7", "Pronghorn", "Speed demon", Rarity.UNCOMMON, "West"),
                Animal("w_8", "Black Bear", "Forest dweller", Rarity.COMMON, "West"),
                Animal("w_9", "Golden Eagle", "Sky ruler", Rarity.RARE, "West"),
                Animal("w_10", "Wolverine", "Fierce loner", Rarity.EPIC, "West")
            )
        )
    )

    fun getDefaultAnimals(): List<Animal> {
        return listOf(
            Animal("default_1", "Common Animal", "Basic creature", Rarity.COMMON, "Default"),
            Animal("default_2", "Uncommon Animal", "Interesting creature", Rarity.UNCOMMON, "Default"),
            Animal("default_3", "Rare Animal", "Hard to find", Rarity.RARE, "Default"),
            Animal("default_4", "Epic Animal", "Very special", Rarity.EPIC, "Default"),
            Animal("default_5", "Legendary Animal", "Extremely rare", Rarity.LEGENDARY, "Default")
        )
    }

    fun addToCollection(animal: Animal) {
        collection.add(animal)
        saveData()
    }

    fun getCollection(): List<Animal> = collection.toList()

    fun addExploredRegion(region: String) {
        exploredRegions.add(region)
        saveData()
    }

    fun getExploredRegions(): Set<String> = exploredRegions.toSet()

    fun getStats(): UserStats {
        // Now we can use stepPrefs directly
        return UserStats(
            totalSteps = stepPrefs.getInt("total_lifetime_steps", 0),
            animalsCollected = collection.size,
            regionsExplored = exploredRegions.size
        )
    }
}