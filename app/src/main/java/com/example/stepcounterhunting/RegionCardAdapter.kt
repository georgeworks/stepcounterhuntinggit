package com.example.stepcounterhunting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RegionCardAdapter(
    private val regions: List<Region>,
    private var collection: Set<Animal>,  // Use only one collection variable
    private val onRegionSelected: (Region, Int) -> Unit
) : RecyclerView.Adapter<RegionCardAdapter.RegionViewHolder>() {

    private var selectedPosition = 0

    inner class RegionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.region_card)
        val regionName: TextView = view.findViewById(R.id.region_name)
        val regionDescription: TextView = view.findViewById(R.id.region_description)
        val animalCount: TextView = view.findViewById(R.id.animal_count)
        val selectedIndicator: ImageView = view.findViewById(R.id.selected_indicator)
        val regionIcon: ImageView = view.findViewById(R.id.region_icon)
        val regionBackground: RelativeLayout = view.findViewById(R.id.region_background)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_region_card, parent, false)
        return RegionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        val region = regions[position]

        holder.regionName.text = region.name
        holder.regionDescription.text = getRegionDescription(region.name)

        // Calculate caught animals in this region using the collection Set
        val caughtInRegion = collection.count { caughtAnimal ->
            region.animals.any { it.id == caughtAnimal.id }
        }
        val totalInRegion = region.animals.size

        holder.animalCount.text = "$caughtInRegion/$totalInRegion"

        // Update progress color based on completion
        when {
            caughtInRegion == totalInRegion -> {
                holder.animalCount.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                )
            }
            caughtInRegion >= totalInRegion / 2 -> {
                holder.animalCount.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark)
                )
            }
            else -> {
                holder.animalCount.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                )
            }
        }

        // Set region icon
        holder.regionIcon.setImageResource(getRegionIcon(region.name))

        // Set background gradient based on region
        val background = when (region.name) {
            "The Appalachians" -> R.drawable.gradient_background
            "Desert Southwest" -> R.drawable.gradient_background_desert
            "Pacific Northwest" -> R.drawable.gradient_background_ocean
            "Great Plains" -> R.drawable.gradient_background_plains
            "Far North" -> R.drawable.gradient_background_arctic
            else -> R.drawable.gradient_background
        }
        holder.regionBackground.setBackgroundResource(background)

        // Show selected state
        if (position == selectedPosition) {
            holder.selectedIndicator.visibility = View.VISIBLE
            holder.card.cardElevation = 8f
        } else {
            holder.selectedIndicator.visibility = View.GONE
            holder.card.cardElevation = 4f
        }

        // Handle click
        holder.card.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onRegionSelected(region, selectedPosition)
        }
    }

    override fun getItemCount() = regions.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    fun updateCollection(newCollection: Set<Animal>) {
        collection = newCollection
    }

    fun getSelectedRegion(): Region? {
        return if (selectedPosition < regions.size) regions[selectedPosition] else null
    }

    private fun getRegionDescription(regionName: String): String {
        return when (regionName) {
            "The Appalachians" -> "Mountain forests and valleys"
            "Desert Southwest" -> "Arid lands and canyons"
            "Pacific Northwest" -> "Coastal rainforests"
            "Great Plains" -> "Vast grasslands"
            "Far North" -> "Tundra and wilderness"
            else -> "Explore and discover"
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
}