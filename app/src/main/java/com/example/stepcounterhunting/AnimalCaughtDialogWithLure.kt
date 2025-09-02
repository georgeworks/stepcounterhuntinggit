package com.example.stepcounterhunting

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment

class AnimalCaughtDialogWithLure(
    private val animal: Animal,
    private val isDuplicate: Boolean,
    private val wasLureUsed: Boolean,
    private val onDismiss: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_animal_caught_with_lure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val animalImage: ImageView = view.findViewById(R.id.caught_animal_image)
        val animalName: TextView = view.findViewById(R.id.caught_animal_name)
        val animalRarity: TextView = view.findViewById(R.id.caught_animal_rarity)
        val animalDescription: TextView = view.findViewById(R.id.caught_animal_description)
        val animalFunFact: TextView = view.findViewById(R.id.caught_animal_fun_fact)
        val duplicateCard: CardView = view.findViewById(R.id.duplicate_reward_card)
        val lureRewardText: TextView = view.findViewById(R.id.lure_reward_text)
        val lureUsedIndicator: TextView = view.findViewById(R.id.lure_used_indicator)
        val continueButton: Button = view.findViewById(R.id.continue_button)

        animalImage.setImageResource(animal.imageResource)
        animalName.text = animal.name
        animalRarity.text = animal.rarity.displayName
        animalRarity.setTextColor(Color.parseColor(animal.rarity.color))
        animalDescription.text = animal.description
        animalFunFact.text = "Fun Fact: ${animal.funFact}"

        // Show lure used indicator if applicable
        if (wasLureUsed) {
            lureUsedIndicator.visibility = View.VISIBLE
            lureUsedIndicator.text = "🎯 Lure Success!"
        } else {
            lureUsedIndicator.visibility = View.GONE
        }

        // Show duplicate reward if applicable
        if (isDuplicate) {
            duplicateCard.visibility = View.VISIBLE
            val currentLures = DataManager.getLureCount()
            lureRewardText.text = "+1 Lure Earned!\nTotal Lures: $currentLures"
        } else {
            duplicateCard.visibility = View.GONE
        }

        continueButton.setOnClickListener {
            onDismiss()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            resources.getDimensionPixelSize(R.dimen.dialog_width),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}