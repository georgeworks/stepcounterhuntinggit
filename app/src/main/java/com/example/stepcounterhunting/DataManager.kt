package com.example.stepcounterhunting

import android.content.Context
import android.content.SharedPreferences

object DataManager {
    private lateinit var prefs: SharedPreferences
    private lateinit var stepPrefs: SharedPreferences
    private val collection = mutableListOf<Animal>()
    private val exploredRegions = mutableSetOf<String>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)
        stepPrefs = context.getSharedPreferences("StepCounter", Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val collectionString = prefs.getString("collection", "") ?: ""
        if (collectionString.isNotEmpty()) {
            val animalIds = collectionString.split(",")
            collection.clear()
            animalIds.forEach { id ->
                findAnimalById(id)?.let { collection.add(it) }
            }
        }

        val regionsString = prefs.getString("explored_regions", "") ?: ""
        if (regionsString.isNotEmpty()) {
            exploredRegions.clear()
            exploredRegions.addAll(regionsString.split(","))
        }
    }

    private fun saveData() {
        val collectionString = collection.joinToString(",") { it.id }
        prefs.edit().putString("collection", collectionString).apply()

        val regionsString = exploredRegions.joinToString(",")
        prefs.edit().putString("explored_regions", regionsString).apply()
    }

    private fun findAnimalById(id: String): Animal? {
        val allAnimals = usRegions.flatMap { it.animals } + getDefaultAnimals()
        return allAnimals.find { it.id == id }
    }

    fun addToCollection(animal: Animal): Boolean {
        // Check if it's a duplicate
        val isDuplicate = collection.any { it.id == animal.id }

        collection.add(animal)
        saveData()

        // If it's a duplicate, award a lure
        if (isDuplicate) {
            val currentLures = getLureCount()
            setLureCount(currentLures + 1)
        }

        return isDuplicate
    }

    fun getLureCount(): Int {
        return prefs.getInt("lure_count", 0)
    }

    fun setLureCount(count: Int) {
        prefs.edit().putInt("lure_count", count).apply()
    }

    fun useLure() {
        val currentLures = getLureCount()
        if (currentLures > 0) {
            setLureCount(currentLures - 1)
        }
    }

    fun getCollection(): List<Animal> = collection.toList()

    fun addExploredRegion(region: String) {
        exploredRegions.add(region)
        saveData()
    }

    fun getExploredRegions(): Set<String> = exploredRegions.toSet()

    fun getStats(): UserStats {
        return UserStats(
            totalSteps = stepPrefs.getInt("total_lifetime_steps", 0),
            animalsCollected = collection.size,
            regionsExplored = exploredRegions.size
        )
    }

    fun getDefaultAnimals(): List<Animal> {
        return listOf(
            Animal(
                "default_1",
                "Common Animal",
                "Basic creature",
                Rarity.COMMON,
                "Default",
                "This mysterious creature adapts to any environment!"
            ),
            Animal(
                "default_2",
                "Uncommon Animal",
                "Interesting creature",
                Rarity.UNCOMMON,
                "Default",
                "Scientists are still discovering new facts about this species!"
            ),
            Animal(
                "default_3",
                "Rare Animal",
                "Hard to find",
                Rarity.RARE,
                "Default",
                "Only a few hundred of these animals have ever been documented!"
            ),
            Animal(
                "default_4",
                "Epic Animal",
                "Very special",
                Rarity.EPIC,
                "Default",
                "This creature has abilities that seem almost supernatural!"
            ),
            Animal(
                "default_5",
                "Legendary Animal",
                "Extremely rare",
                Rarity.LEGENDARY,
                "Default",
                "Some say this animal doesn't exist, but you've proven them wrong!"
            )
        )
    }

    val usRegions = listOf(
        Region(
            "Northeast",
            listOf(
                Animal(
                    "ne_1",
                    "White-tailed Deer",
                    "Common forest dweller",
                    Rarity.COMMON,
                    "Northeast",
                    "White-tailed deer can jump up to 10 feet high and leap 30 feet in a single bound!"
                ),
                Animal(
                    "ne_2",
                    "Black Bear",
                    "Large omnivore",
                    Rarity.UNCOMMON,
                    "Northeast",
                    "Black bears have better eyesight and hearing than humans, and their sense of smell is 7 times greater than a dog's!"
                ),
                Animal(
                    "ne_3",
                    "Wild Turkey",
                    "Native game bird",
                    Rarity.COMMON,
                    "Northeast",
                    "Wild turkeys can fly up to 55 mph in short bursts and run at speeds up to 25 mph!"
                ),
                Animal(
                    "ne_4",
                    "Bobcat",
                    "Elusive predator",
                    Rarity.RARE,
                    "Northeast",
                    "Bobcats can leap 12 feet horizontally and their back feet step in the exact same spot where their front feet stepped!"
                ),
                Animal(
                    "ne_5",
                    "Moose",
                    "Largest deer species",
                    Rarity.EPIC,
                    "Northeast",
                    "Moose can dive up to 20 feet underwater to feed on aquatic plants and can hold their breath for a full minute!"
                ),
                Animal(
                    "ne_6",
                    "Eastern Cougar",
                    "Mythical big cat",
                    Rarity.LEGENDARY,
                    "Northeast",
                    "Eastern cougars were declared extinct in 2018, but unconfirmed sightings still occur, making them truly legendary!"
                ),
                Animal(
                    "ne_7",
                    "Red Fox",
                    "Cunning canine",
                    Rarity.COMMON,
                    "Northeast",
                    "Red foxes use Earth's magnetic field to hunt, pouncing in a north-easterly direction with 73% success rate!"
                ),
                Animal(
                    "ne_8",
                    "River Otter",
                    "Playful swimmer",
                    Rarity.UNCOMMON,
                    "Northeast",
                    "River otters can hold their breath for up to 8 minutes underwater and close their ears and nostrils when diving!"
                ),
                Animal(
                    "ne_9",
                    "Bald Eagle",
                    "National bird",
                    Rarity.RARE,
                    "Northeast",
                    "Bald eagles can see up to 8 times more clearly than humans and can spot a rabbit from 3 miles away!"
                ),
                Animal(
                    "ne_10",
                    "Gray Wolf",
                    "Pack hunter",
                    Rarity.LEGENDARY,
                    "Northeast",
                    "Gray wolves can sprint at 40 mph and have a bite force of 1,500 pounds per square inch!"
                )
            )
        ),
        Region(
            "Southeast",
            listOf(
                Animal(
                    "se_1",
                    "Alligator",
                    "Swamp predator",
                    Rarity.UNCOMMON,
                    "Southeast",
                    "Alligators can go through 3,000 teeth in their lifetime and their bite force is 2,980 psi!"
                ),
                Animal(
                    "se_2",
                    "Armadillo",
                    "Armored mammal",
                    Rarity.COMMON,
                    "Southeast",
                    "Nine-banded armadillos always give birth to identical quadruplets and can hold their breath for 6 minutes!"
                ),
                Animal(
                    "se_3",
                    "Wild Boar",
                    "Invasive species",
                    Rarity.COMMON,
                    "Southeast",
                    "Wild boars can run up to 30 mph and their tusks never stop growing throughout their lives!"
                ),
                Animal(
                    "se_4",
                    "Florida Panther",
                    "Endangered cat",
                    Rarity.LEGENDARY,
                    "Southeast",
                    "Only 120-230 Florida panthers remain in the wild, and they can leap 15 feet vertically!"
                ),
                Animal(
                    "se_5",
                    "Manatee",
                    "Gentle giant",
                    Rarity.EPIC,
                    "Southeast",
                    "Manatees have no natural predators and can eat 10-15% of their body weight daily - up to 150 pounds of plants!"
                ),
                Animal(
                    "se_6",
                    "Cottonmouth",
                    "Venomous snake",
                    Rarity.UNCOMMON,
                    "Southeast",
                    "Cottonmouths are the only venomous snake in North America that lives in water and gives live birth!"
                ),
                Animal(
                    "se_7",
                    "Raccoon",
                    "Masked bandit",
                    Rarity.COMMON,
                    "Southeast",
                    "Raccoons can remember solutions to tasks for up to 3 years and their hands are so sensitive they can identify objects without seeing them!"
                ),
                Animal(
                    "se_8",
                    "Great Blue Heron",
                    "Tall wader",
                    Rarity.UNCOMMON,
                    "Southeast",
                    "Great blue herons have specialized neck vertebrae that allow them to strike prey at lightning speed!"
                ),
                Animal(
                    "se_9",
                    "Black Bear",
                    "Forest dweller",
                    Rarity.RARE,
                    "Southeast",
                    "Black bears in the Southeast are smaller than northern bears but are excellent swimmers and have been found swimming to islands miles offshore!"
                ),
                Animal(
                    "se_10",
                    "Red Wolf",
                    "Critically endangered",
                    Rarity.LEGENDARY,
                    "Southeast",
                    "Red wolves are the world's most endangered canid with only about 15-17 left in the wild, all in North Carolina!"
                )
            )
        ),
        Region(
            "Midwest",
            listOf(
                Animal(
                    "mw_1",
                    "Bison",
                    "Prairie giant",
                    Rarity.EPIC,
                    "Midwest",
                    "Bison can run 35 mph, jump 6 feet high, and are North America's largest land animal weighing up to 2,000 pounds!"
                ),
                Animal(
                    "mw_2",
                    "Prairie Dog",
                    "Social rodent",
                    Rarity.COMMON,
                    "Midwest",
                    "Prairie dogs have a complex language with over 100 words and can describe predator details to their colony!"
                ),
                Animal(
                    "mw_3",
                    "Coyote",
                    "Adaptable predator",
                    Rarity.COMMON,
                    "Midwest",
                    "Coyotes can run up to 43 mph and have expanded their range by 40% since the 1950s!"
                ),
                Animal(
                    "mw_4",
                    "Badger",
                    "Fierce digger",
                    Rarity.UNCOMMON,
                    "Midwest",
                    "Badgers can dig faster than a human with a shovel and sometimes team up with coyotes to hunt!"
                ),
                Animal(
                    "mw_5",
                    "Elk",
                    "Large ungulate",
                    Rarity.RARE,
                    "Midwest",
                    "Male elk antlers can grow up to 1 inch per day during summer and weigh up to 40 pounds!"
                ),
                Animal(
                    "mw_6",
                    "Golden Eagle",
                    "Powerful raptor",
                    Rarity.RARE,
                    "Midwest",
                    "Golden eagles can reach diving speeds of 200 mph and have grip strength of 750 psi - 15 times stronger than a human hand!"
                ),
                Animal(
                    "mw_7",
                    "White-tailed Deer",
                    "Common herbivore",
                    Rarity.COMMON,
                    "Midwest",
                    "A white-tailed deer's stomach has four chambers and they can eat over 600 different types of plants!"
                ),
                Animal(
                    "mw_8",
                    "River Otter",
                    "Aquatic mammal",
                    Rarity.UNCOMMON,
                    "Midwest",
                    "River otters can swim at 7 mph and use tools like rocks to crack open shellfish!"
                ),
                Animal(
                    "mw_9",
                    "Sandhill Crane",
                    "Tall migrant",
                    Rarity.RARE,
                    "Midwest",
                    "Sandhill cranes are one of the oldest bird species, with fossils dating back 2.5 million years!"
                ),
                Animal(
                    "mw_10",
                    "Mountain Lion",
                    "Apex predator",
                    Rarity.LEGENDARY,
                    "Midwest",
                    "Mountain lions can leap 40 feet horizontally and 15 feet vertically, and their territory can span 300 square miles!"
                )
            )
        ),
        Region(
            "West",
            listOf(
                Animal(
                    "w_1",
                    "Grizzly Bear",
                    "Massive predator",
                    Rarity.LEGENDARY,
                    "West",
                    "Grizzly bears can smell food from 18 miles away and run as fast as a horse despite weighing up to 800 pounds!"
                ),
                Animal(
                    "w_2",
                    "Mountain Goat",
                    "Cliff climber",
                    Rarity.RARE,
                    "West",
                    "Mountain goats aren't true goats but are more closely related to antelopes, and can climb slopes with 60-degree inclines!"
                ),
                Animal(
                    "w_3",
                    "Mule Deer",
                    "Large-eared deer",
                    Rarity.COMMON,
                    "West",
                    "Mule deer can jump 8 feet high and 24 feet long, and their large ears can move independently to detect predators!"
                ),
                Animal(
                    "w_4",
                    "Bighorn Sheep",
                    "Horned climber",
                    Rarity.UNCOMMON,
                    "West",
                    "Bighorn sheep rams' horns can weigh 30 pounds, and they charge at each other at 40 mph during mating battles!"
                ),
                Animal(
                    "w_5",
                    "California Condor",
                    "Giant bird",
                    Rarity.LEGENDARY,
                    "West",
                    "California condors have the largest wingspan of any North American bird at 10 feet and can soar for hours without flapping!"
                ),
                Animal(
                    "w_6",
                    "Gray Wolf",
                    "Pack hunter",
                    Rarity.EPIC,
                    "West",
                    "Gray wolves can travel 30 miles a day while hunting and howl at 90-115 decibels - as loud as a rock concert!"
                ),
                Animal(
                    "w_7",
                    "Pronghorn",
                    "Speed demon",
                    Rarity.UNCOMMON,
                    "West",
                    "Pronghorns are the second-fastest land animal at 60 mph and have the largest eyes relative to body size of any North American mammal!"
                ),
                Animal(
                    "w_8",
                    "Black Bear",
                    "Forest dweller",
                    Rarity.COMMON,
                    "West",
                    "Western black bears can be black, brown, cinnamon, or even blonde in color, and can smell food from 20 miles away!"
                ),
                Animal(
                    "w_9",
                    "Golden Eagle",
                    "Sky ruler",
                    Rarity.RARE,
                    "West",
                    "Golden eagles have been recorded carrying prey weighing up to 15 pounds while flying!"
                ),
                Animal(
                    "w_10",
                    "Wolverine",
                    "Fierce loner",
                    Rarity.EPIC,
                    "West",
                    "Wolverines can travel 15 miles a day in search of food and have jaws powerful enough to bite through frozen bones!"
                )
            )
        )
    )
}