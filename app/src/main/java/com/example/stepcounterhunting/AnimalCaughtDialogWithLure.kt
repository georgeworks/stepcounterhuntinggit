package com.example.stepcounterhunting

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import android.view.WindowManager


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

        // Apply rarity-based background to the dialog's root view
        applyRarityBackground(view as ViewGroup, animal.rarity)

        // Apply haptic feedback based on rarity
        applyHapticFeedback(animal.rarity)

        // Set animal details
        animalImage.setImageResource(animal.imageResource)
        animalName.text = animal.name
        animalRarity.text = animal.rarity.displayName
        animalRarity.setTextColor(Color.parseColor(animal.rarity.color))
        animalDescription.text = animal.description
        animalFunFact.text = animal.funFact

        // Animate the animal image entrance
        animateAnimalEntrance(animalImage, animal.rarity)

        // Animate text elements with delays
        animateTextElements(listOf(animalName, animalRarity, animalDescription))

        // Show lure used indicator if applicable
        if (wasLureUsed) {
            lureUsedIndicator.visibility = View.VISIBLE
            lureUsedIndicator.text = "🎯 Lure Success!"
            // Animate lure indicator
            lureUsedIndicator.alpha = 0f
            lureUsedIndicator.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(800)
                .start()
        } else {
            lureUsedIndicator.visibility = View.GONE
        }

        // Show duplicate reward if applicable
        if (isDuplicate) {
            duplicateCard.visibility = View.VISIBLE
            val currentLures = DataManager.getLureCount()
            lureRewardText.text = "+1 Lure Earned!\nTotal Lures: $currentLures"

            // Animate duplicate card
            duplicateCard.alpha = 0f
            duplicateCard.scaleX = 0.8f
            duplicateCard.scaleY = 0.8f
            duplicateCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(1000)
                .start()
        } else {
            duplicateCard.visibility = View.GONE
        }

        continueButton.setOnClickListener {
            onDismiss()
            dismiss()
        }

        // Animate continue button
        continueButton.alpha = 0f
        continueButton.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1200)
            .start()
    }

    private fun applyRarityBackground(rootView: ViewGroup, rarity: Rarity) {
        val colors = when(rarity) {
            Rarity.LEGENDARY -> intArrayOf(
                Color.parseColor("#FFE4B5"), // Moccasin
                Color.parseColor("#FFD700"), // Gold
                Color.parseColor("#FFA500")  // Orange
            )
            Rarity.RARE -> intArrayOf(
                Color.parseColor("#E1BEE7"), // Light purple
                Color.parseColor("#BA68C8"), // Medium purple
                Color.parseColor("#9C27B0")  // Deep purple
            )
            Rarity.UNCOMMON -> intArrayOf(
                Color.parseColor("#B3E5FC"), // Light blue
                Color.parseColor("#4FC3F7"), // Medium blue
                Color.parseColor("#29B6F6")  // Deeper blue
            )
            Rarity.COMMON -> intArrayOf(
                Color.parseColor("#F5F5F5"), // Light gray
                Color.parseColor("#E0E0E0"), // Medium gray
                Color.parseColor("#BDBDBD")  // Darker gray
            )
        }

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            colors
        )
        gradientDrawable.cornerRadius = 32f

        // Apply to the CardView's background
        rootView.background = gradientDrawable
    }

    private fun animateAnimalEntrance(imageView: ImageView, rarity: Rarity) {
        // Start with image invisible and small
        imageView.alpha = 0f
        imageView.scaleX = 0.3f
        imageView.scaleY = 0.3f

        // Different animation based on rarity
        when(rarity) {
            Rarity.LEGENDARY -> {
                // Dramatic spin and scale for legendary
                imageView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(360f)
                    .setDuration(800)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .start()
            }
            Rarity.RARE -> {
                // Bounce effect for rare
                imageView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            }
            else -> {
                // Simple fade and scale for common/uncommon
                imageView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
        }
    }

    private fun animateTextElements(textViews: List<TextView>) {
        textViews.forEachIndexed { index, textView ->
            textView.alpha = 0f
            textView.translationY = 20f
            textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(500L + (index * 100L))
                .start()
        }
    }

    private fun applyHapticFeedback(rarity: Rarity) {
        try {
            val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = when(rarity) {
                        Rarity.LEGENDARY -> {
                            // Epic pattern - multiple strong pulses
                            longArrayOf(0, 100, 100, 100, 100, 300, 100, 100)
                        }
                        Rarity.RARE -> {
                            // Special pattern - double pulse
                            longArrayOf(0, 80, 80, 150)
                        }
                        Rarity.UNCOMMON -> {
                            // Medium pulse
                            longArrayOf(0, 60)
                        }
                        Rarity.COMMON -> {
                            // Light pulse
                            longArrayOf(0, 30)
                        }
                    }

                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    it.vibrate(effect)
                } else {
                    // Fallback for older devices
                    @Suppress("DEPRECATION")
                    when(rarity) {
                        Rarity.LEGENDARY -> it.vibrate(longArrayOf(0, 100, 100, 100, 100, 300), -1)
                        Rarity.RARE -> it.vibrate(longArrayOf(0, 80, 80, 150), -1)
                        Rarity.UNCOMMON -> it.vibrate(60)
                        Rarity.COMMON -> it.vibrate(30)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Vibration permission not granted, silently skip
            e.printStackTrace()
        } catch (e: Exception) {
            // Any other error, silently skip
            e.printStackTrace()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)  // Add this - don't use STYLE_NO_FRAME
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            // Use smaller width - 300dp instead of 360dp
            val width = (300 * resources.displayMetrics.density).toInt()

            setLayout(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setBackgroundDrawableResource(android.R.color.transparent)

            // Add dim effect
            setDimAmount(0.7f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog?.setCanceledOnTouchOutside(false)
    }
}