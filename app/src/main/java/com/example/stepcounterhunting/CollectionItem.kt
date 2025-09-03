package com.example.stepcounterhunting

sealed class CollectionItem {
    data class Header(val regionName: String, val caughtCount: Int) : CollectionItem()
    data class AnimalCard(
        val animal: Animal,
        val isCaught: Boolean,
        val count: Int = 0  // Added count parameter with default value
    ) : CollectionItem()
}