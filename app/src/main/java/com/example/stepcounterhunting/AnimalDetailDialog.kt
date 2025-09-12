package com.example.stepcounterhunting

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class AnimalDetailDialog(
    private val animal: Animal
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Choose layout based on animal
        return if (animal.id == "app_6" || animal.id == "app_1") {
            inflater.inflate(R.layout.dialog_animal_detail_overlay, container, false)
        } else {
            inflater.inflate(R.layout.dialog_animal_detail, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val animalImage: ImageView = view.findViewById(R.id.detail_animal_image)
        val animalName: TextView = view.findViewById(R.id.detail_animal_name)
        val animalRarity: TextView? = view.findViewById(R.id.detail_animal_rarity)
        val animalDescription: TextView? = view.findViewById(R.id.detail_animal_description)
        val animalFunFact: TextView? = view.findViewById(R.id.detail_animal_fun_fact)

        when (animal.id) {
            "app_6" -> {
                // Hellbender with overlay layout
                animalImage.setImageResource(R.drawable.hellbenderfull)
                animalName.text = animal.name

                // Only set these if they exist in your overlay layout
                animalRarity?.text = animal.rarity.displayName
                animalDescription?.text = animal.description
                animalFunFact?.text = "${animal.funFact}"
            }
            "app_1" -> {
                // Brook trout with overlay layout
                animalImage.setImageResource(R.drawable.brooktrout)
                animalName.text = animal.name

                // Only set these if they exist in your overlay layout
                animalRarity?.text = animal.rarity.displayName
                animalDescription?.text = animal.description
                animalFunFact?.text = "${animal.funFact}"
            }
            else -> {
                // Normal animals with standard layout
                animalImage.setImageResource(animal.imageResource)
                animalName.text = animal.name
                animalRarity?.text = animal.rarity.displayName
                animalRarity?.setTextColor(Color.parseColor(animal.rarity.color))
                animalDescription?.text = animal.description
                animalFunFact?.text = "${animal.funFact}"
            }
        }

        view.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        return dialog
    }
}