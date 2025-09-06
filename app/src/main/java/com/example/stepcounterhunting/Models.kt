package com.example.stepcounterhunting

data class Animal(
    val id: String,
    val name: String,
    val description: String,
    val rarity: Rarity,
    val region: String,
    val funFact: String,  // Added fun fact field
    val imageResource: Int = R.drawable.ic_animal_placeholder
)

enum class Rarity(val displayName: String, val weight: Int, val color: String) {
    COMMON("Common", 40, "#808080"),
    UNCOMMON("Uncommon", 30, "#00FF00"),
    RARE("Rare", 20, "#0080FF"),
    LEGENDARY("Legendary", 2, "#FFD700")
}

data class Region(
    val name: String,
    val animals: List<Animal>
)

data class UserStats(
    val totalSteps: Int,
    val animalsCollected: Int,
    val regionsExplored: Int
)