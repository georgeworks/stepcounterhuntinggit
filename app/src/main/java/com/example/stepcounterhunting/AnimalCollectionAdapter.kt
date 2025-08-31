package com.example.stepcounterhunting

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class AnimalCollectionAdapter(
    private var animals: List<Animal>,
    private val onAnimalClick: (Animal) -> Unit  // Add click callback
) : RecyclerView.Adapter<AnimalCollectionAdapter.AnimalViewHolder>() {

    class AnimalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.animal_card)
        val imageView: ImageView = view.findViewById(R.id.animal_image)
        val nameText: TextView = view.findViewById(R.id.animal_name)
        val rarityText: TextView = view.findViewById(R.id.animal_rarity)
        val regionText: TextView = view.findViewById(R.id.animal_region)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_animal_collection, parent, false)
        return AnimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        val animal = animals[position]

        holder.nameText.text = animal.name
        holder.rarityText.text = animal.rarity.displayName
        holder.regionText.text = animal.region

        // Set rarity color
        holder.cardView.setCardBackgroundColor(Color.parseColor(animal.rarity.color))

        // Set placeholder image
        holder.imageView.setImageResource(animal.imageResource)

        // Add click listener to the entire card
        holder.cardView.setOnClickListener {
            onAnimalClick(animal)
        }

        // Add ripple effect on click (visual feedback)
        holder.cardView.isClickable = true
        holder.cardView.isFocusable = true
    }

    override fun getItemCount() = animals.size

    fun updateData(newAnimals: List<Animal>) {
        animals = newAnimals
        notifyDataSetChanged()
    }
}