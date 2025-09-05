package com.example.stepcounterhunting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CollectionFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnimalCollectionAdapter
    private lateinit var totalProgressText: TextView
    private lateinit var countryFilterSpinner: Spinner
    private var selectedFilter = "United States"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.collection_recycler_view)
        totalProgressText = view.findViewById(R.id.total_progress)
        countryFilterSpinner = view.findViewById(R.id.country_filter_spinner)

        setupFilterSpinner()

        // Use a GridLayoutManager with span size lookup for headers
        val gridLayoutManager = GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Headers take full width (2 columns), animals take 1 column
                return if (adapter.isHeader(position)) 2 else 1
            }
        }
        recyclerView.layoutManager = gridLayoutManager

        // Get all animals organized by region with caught status
        val collectionData = organizeCollectionByRegion(selectedFilter)

        // Update total progress
        updateTotalProgress(selectedFilter)

        // Pass click listener to adapter
        adapter = AnimalCollectionAdapter(collectionData) { animal ->
            if (animal.isCaught) {
                showAnimalDetails(animal.animal)
            } else {
                // Show hint for uncaught animals
                android.widget.Toast.makeText(
                    context,
                    "Keep hunting in ${animal.animal.region} to find this animal!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun setupFilterSpinner() {
        // Create custom adapter to show "(coming soon)" for unavailable countries
        val filterOptions = listOf(
            "United States",
            "China (coming soon)",
            "Australia (coming soon)",
            "Brazil (coming soon)",
            "Madagascar (coming soon)"
        )

        // Create custom ArrayAdapter to handle disabled items
        val filterAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
        ) {
            override fun isEnabled(position: Int): Boolean {
                // Only United States (position 0) is enabled
                return position == 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as? TextView

                if (position != 0) {
                    // Gray out disabled items
                    textView?.setTextColor(android.graphics.Color.GRAY)
                } else {
                    // Normal color for enabled item
                    textView?.setTextColor(android.graphics.Color.BLACK)
                }

                return view
            }
        }

        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countryFilterSpinner.adapter = filterAdapter

        // Set default to "United States" (position 0)
        countryFilterSpinner.setSelection(0)

        countryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Only allow selecting United States
                if (position == 0) {
                    selectedFilter = "United States"
                    refreshCollection()
                } else {
                    // If user tries to select a "coming soon" country, show message and revert to United States
                    android.widget.Toast.makeText(
                        context,
                        "This country is coming soon!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Reset to United States
                    countryFilterSpinner.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun refreshCollection() {
        val collectionData = organizeCollectionByRegion(selectedFilter)
        adapter.updateData(collectionData)
        updateTotalProgress(selectedFilter)
    }

    private fun organizeCollectionByRegion(filter: String): List<CollectionItem> {
        val allCaughtAnimals = DataManager.getCollection()
        // Get unique animals only for display (but keep count information)
        val uniqueCaughtAnimals = allCaughtAnimals.distinctBy { it.id }
        val caughtAnimalIds = uniqueCaughtAnimals.map { it.id }.toSet()

        // Create a map of animal counts for showing duplicates
        val animalCounts = allCaughtAnimals.groupingBy { it.id }.eachCount()

        val items = mutableListOf<CollectionItem>()

        when (filter) {
            "United States" -> {
                // Show only US regions
                addUSRegions(items, caughtAnimalIds, animalCounts)
            }
            else -> {
                // Show specific country (though this shouldn't be reached now)
                addSpecificCountry(items, filter, uniqueCaughtAnimals, animalCounts)
            }
        }

        return items
    }

    private fun addUSRegions(items: MutableList<CollectionItem>, caughtAnimalIds: Set<String>, animalCounts: Map<String, Int>) {
        DataManager.usRegions.forEach { region ->
            // Add region header
            items.add(CollectionItem.Header(region.name, countCaughtInRegion(region.animals, caughtAnimalIds)))

            // Add all animals from this region (caught and uncaught)
            region.animals.forEach { animal ->
                val isCaught = caughtAnimalIds.contains(animal.id)
                val count = if (isCaught) animalCounts[animal.id] ?: 1 else 0
                items.add(CollectionItem.AnimalCard(animal, isCaught, count))
            }
        }
    }

    private fun addOtherCountries(items: MutableList<CollectionItem>, uniqueCaughtAnimals: List<Animal>, animalCounts: Map<String, Int>) {
        val otherCountries = listOf("China", "Australia", "Brazil", "Madagascar")
        otherCountries.forEach { country ->
            val caughtInCountry = uniqueCaughtAnimals.filter { it.region.contains(country) }

            if (caughtInCountry.isNotEmpty()) {
                items.add(CollectionItem.Header(country, caughtInCountry.size))

                caughtInCountry.forEach { animal ->
                    val count = animalCounts[animal.id] ?: 1
                    items.add(CollectionItem.AnimalCard(animal, true, count))
                }
            }
        }
    }

    private fun addSpecificCountry(items: MutableList<CollectionItem>, country: String, uniqueCaughtAnimals: List<Animal>, animalCounts: Map<String, Int>) {
        // For non-US countries, we show default animals as placeholders
        val defaultAnimals = DataManager.getDefaultAnimals()
        val caughtInCountry = uniqueCaughtAnimals.filter {
            when (country) {
                "China" -> it.region.contains("China")
                "Australia" -> it.region.contains("Australia")
                "Brazil" -> it.region.contains("Brazil")
                "Madagascar" -> it.region.contains("Madagascar") || it.region.contains("England") ||
                        it.region.contains("Scotland") || it.region.contains("Wales") ||
                        it.region.contains("Northern Ireland")
                else -> false
            }
        }
        val caughtIds = caughtInCountry.map { it.id }.toSet()

        // Add country header
        val totalPossible = if (country == "United States") 40 else 5
        items.add(CollectionItem.Header(country, caughtInCountry.size))

        // Show caught animals
        caughtInCountry.forEach { animal ->
            val count = animalCounts[animal.id] ?: 1
            items.add(CollectionItem.AnimalCard(animal, true, count))
        }

        // Show uncaught placeholders for non-US countries (using default animals as templates)
        if (country != "United States") {
            val uncaughtCount = 5 - caughtInCountry.size
            if (uncaughtCount > 0) {
                defaultAnimals.take(uncaughtCount).forEach { templateAnimal ->
                    // Create placeholder animals for uncaught slots
                    val placeholderAnimal = Animal(
                        "${country.lowercase()}_unknown_${templateAnimal.id}",
                        "Unknown",
                        "Not discovered yet",
                        templateAnimal.rarity,
                        country,
                        "Explore $country to discover this animal!",
                        templateAnimal.imageResource
                    )
                    items.add(CollectionItem.AnimalCard(placeholderAnimal, false, 0))
                }
            }
        }
    }

    private fun countCaughtInRegion(regionAnimals: List<Animal>, caughtIds: Set<String>): Int {
        return regionAnimals.count { caughtIds.contains(it.id) }
    }

    private fun updateTotalProgress(filter: String) {
        val allCaughtAnimals = DataManager.getCollection()
        // Get unique animals for accurate count
        val uniqueCaughtAnimals = allCaughtAnimals.distinctBy { it.id }

        val (caught, total) = when (filter) {
            "United States" -> {
                val usCaught = uniqueCaughtAnimals.count { animal ->  // Count unique animals
                    DataManager.usRegions.any { region ->
                        region.animals.any { it.id == animal.id }
                    }
                }
                Pair(usCaught, 40)
            }
            else -> {
                val countryCaught = uniqueCaughtAnimals.count { animal ->  // Count unique animals
                    when (filter) {
                        "China" -> animal.region.contains("China")
                        "Australia" -> animal.region.contains("Australia")
                        "Brazil" -> animal.region.contains("Brazil")
                        "Madagascar" -> animal.region.contains("Madagascar") ||
                                animal.region.contains("England") ||
                                animal.region.contains("Scotland") ||
                                animal.region.contains("Wales") ||
                                animal.region.contains("Northern Ireland")
                        else -> false
                    }
                }
                Pair(countryCaught, 5) // Assuming 5 animals per non-US country
            }
        }

        totalProgressText.text = "$caught / $total"
    }

    private fun showAnimalDetails(animal: Animal) {
        // Get the count of how many times this animal was caught
        val allCaughtAnimals = DataManager.getCollection()
        val count = allCaughtAnimals.count { it.id == animal.id }

        // Pass the count to the dialog if you want to show it
        val dialog = AnimalDetailDialog(animal, )
        dialog.show(childFragmentManager, "animal_detail")
    }

    override fun onResume() {
        super.onResume()
        // Refresh the collection when returning to this screen
        refreshCollection()
    }
}