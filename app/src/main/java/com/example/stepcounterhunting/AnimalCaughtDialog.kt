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

class AnimalCaughtDialog(
    private val animal: Animal,
    private val onDismiss: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_animal_caught, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val animalImage: ImageView = view.findViewById(R.id.caught_animal_image)
        val animalName: TextView = view.findViewById(R.id.caught_animal_name)
        val animalRarity: TextView = view.findViewById(R.id.caught_animal_rarity)
        val animalDescription: TextView = view.findViewById(R.id.caught_animal_description)
        val continueButton: Button = view.findViewById(R.id.continue_button)

        animalImage.setImageResource(animal.imageResource)
        animalName.text = animal.name
        animalRarity.text = animal.rarity.displayName
        animalRarity.setTextColor(Color.parseColor(animal.rarity.color))
        animalDescription.text = animal.description

        continueButton.setOnClickListener {
            onDismiss()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}