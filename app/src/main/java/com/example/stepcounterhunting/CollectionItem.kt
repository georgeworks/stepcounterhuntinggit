package com.example.stepcounterhunting

sealed class CollectionItem {
    data class Header(val regionName: String, val caughtCount: Int) : CollectionItem()
    data class AnimalCard(val animal: Animal, val isCaught: Boolean) : CollectionItem()
}