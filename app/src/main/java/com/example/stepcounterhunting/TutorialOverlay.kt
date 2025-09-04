package com.example.stepcounterhunting

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class TutorialOverlay(private val context: Context, private val rootView: ViewGroup) {

    private var currentStep = 0
    private var overlayView: View? = null
    private var tutorialCallback: (() -> Unit)? = null
    private val prefs = context.getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

    companion object {
        private var activeInstance: TutorialOverlay? = null
    }

    data class TutorialStep(
        val targetViewId: Int?,
        val title: String,
        val description: String,
        val position: Position = Position.BELOW,
        val delay: Long = 0L
    )

    enum class Position {
        ABOVE, BELOW, LEFT, RIGHT
    }

    private val tutorialSteps = listOf(
        TutorialStep(
            targetViewId = R.id.country_spinner,
            title = "Select Your Country",
            description = "Start by choosing the country you want to hunt in. Different countries have unique animals to discover!",
            position = Position.BELOW,
            delay = 500L
        ),
        TutorialStep(
            targetViewId = R.id.region_spinner,
            title = "Choose a Region",
            description = "Each country has different regions with their own wildlife. Select a specific region to explore.",
            position = Position.BELOW,
            delay = 300L
        ),
        TutorialStep(
            targetViewId = R.id.start_hunt_button,
            title = "Start Your Hunt",
            description = "Once you've selected where to hunt, tap this button to begin! You'll need to walk 100 steps to catch an animal.",
            position = Position.ABOVE,
            delay = 200L
        ),
        TutorialStep(
            targetViewId = R.id.lure_count_text,
            title = "Lures Boost Rare Finds",
            description = "When you catch duplicate animals, you'll earn lures. Use them to increase your chances of finding rare animals!",
            position = Position.ABOVE,
            delay = 200L
        ),
        TutorialStep(
            targetViewId = R.id.region_progress_text,
            title = "Track Your Progress",
            description = "This shows how many unique animals you've caught in the selected region. Try to catch them all!",
            position = Position.ABOVE,
            delay = 200L
        )
    )

    fun startTutorial(onComplete: () -> Unit) {
        // Clean up any existing instance first
        activeInstance?.cleanup()
        activeInstance = this

        tutorialCallback = onComplete

        // Check if tutorial was in progress
        val wasInProgress = prefs.getBoolean("tutorial_in_progress", false)
        val savedStep = prefs.getInt("tutorial_current_step", 0)

        // If tutorial was interrupted, resume from where they left off
        if (wasInProgress && savedStep > 0 && savedStep < tutorialSteps.size) {
            currentStep = savedStep
        } else {
            currentStep = 0
        }

        // Mark tutorial as in progress
        prefs.edit()
            .putBoolean("tutorial_in_progress", true)
            .putInt("tutorial_current_step", currentStep)
            .apply()

        // Add initial delay to let UI settle
        rootView.postDelayed({
            showStep(currentStep)
        }, 800L)
    }

    fun cleanup() {
        // Remove any existing overlay
        overlayView?.let {
            try {
                rootView.removeView(it)
            } catch (e: Exception) {
                // View might already be removed
            }
            overlayView = null
        }

        // Clear active instance if it's this one
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    fun isActive(): Boolean {
        return overlayView != null && overlayView?.parent != null
    }

    private fun showStep(stepIndex: Int) {
        if (stepIndex >= tutorialSteps.size) {
            completeTutorial()
            return
        }

        // Save current progress
        prefs.edit().putInt("tutorial_current_step", stepIndex).apply()

        val step = tutorialSteps[stepIndex]

        // Add delay if specified
        if (step.delay > 0) {
            rootView.postDelayed({
                displayStep(step, stepIndex)
            }, step.delay)
        } else {
            displayStep(step, stepIndex)
        }
    }

    private fun displayStep(step: TutorialStep, stepIndex: Int) {
        // Remove previous overlay if exists
        overlayView?.let {
            rootView.removeView(it)
            overlayView = null
        }

        // Create new overlay
        overlayView = LayoutInflater.from(context).inflate(R.layout.tutorial_overlay, rootView, false)

        val titleText = overlayView?.findViewById<TextView>(R.id.tutorial_title)
        val descText = overlayView?.findViewById<TextView>(R.id.tutorial_description)
        val nextButton = overlayView?.findViewById<Button>(R.id.tutorial_next_button)
        val skipButton = overlayView?.findViewById<Button>(R.id.tutorial_skip_button)
        val highlightView = overlayView?.findViewById<HighlightView>(R.id.highlight_view)

        titleText?.text = step.title
        descText?.text = step.description

        // Update button text based on position in tutorial
        if (stepIndex == tutorialSteps.size - 1) {
            nextButton?.text = "Finish"
        } else {
            nextButton?.text = "Next (${stepIndex + 1}/${tutorialSteps.size})"
        }

        // Position the highlight and bubble
        if (step.targetViewId != null) {
            val targetView = rootView.findViewById<View>(step.targetViewId)
            if (targetView != null && targetView.visibility == View.VISIBLE) {
                // Wait for layout to complete before positioning
                targetView.post {
                    positionHighlight(highlightView, targetView, step.position)
                }
            }
        }

        // Always show both buttons for walkthrough style
        nextButton?.visibility = View.VISIBLE
        skipButton?.visibility = View.VISIBLE

        nextButton?.setOnClickListener {
            nextStep()
        }

        skipButton?.setOnClickListener {
            showSkipConfirmation()
        }

        // Make overlay clickable to prevent interactions with underlying views
        overlayView?.isClickable = true
        overlayView?.isFocusable = true

        rootView.addView(overlayView)
    }

    private fun showSkipConfirmation() {
        // Optional: You can add a confirmation dialog here
        // For now, just complete the tutorial
        completeTutorial()
    }

    private fun positionHighlight(highlightView: HighlightView?, targetView: View, position: Position) {
        highlightView?.post {
            val location = IntArray(2)
            targetView.getLocationInWindow(location)

            val rootLocation = IntArray(2)
            rootView.getLocationInWindow(rootLocation)

            // Calculate relative position with extra padding
            val padding = 15f
            val relativeLeft = location[0] - rootLocation[0] - padding
            val relativeTop = location[1] - rootLocation[1] - padding
            val relativeRight = relativeLeft + targetView.width + (padding * 2)
            val relativeBottom = relativeTop + targetView.height + (padding * 2)

            highlightView.setHighlight(
                relativeLeft,
                relativeTop,
                relativeRight,
                relativeBottom
            )

            // Position the tutorial bubble
            positionBubble(position, relativeLeft, relativeTop, relativeRight, relativeBottom)
        }
    }

    private fun positionBubble(position: Position, left: Float, top: Float, right: Float, bottom: Float) {
        val bubble = overlayView?.findViewById<LinearLayout>(R.id.tutorial_bubble)
        bubble?.let {
            val params = it.layoutParams as FrameLayout.LayoutParams
            val margin = 20

            // Get the actual usable screen dimensions
            val displayMetrics = context.resources.displayMetrics
            val screenHeight = rootView.height
            val screenWidth = rootView.width

            // Reserve space for bottom navigation (typically 56-80dp)
            val bottomNavHeight = 300 // Increased for better safety
            val topSafeArea = 100 // Space for status bar
            val usableHeight = screenHeight - bottomNavHeight - topSafeArea

            // Measure the bubble to get its dimensions
            it.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth - margin * 2, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(usableHeight, View.MeasureSpec.AT_MOST)
            )
            val bubbleHeight = it.measuredHeight
            val bubbleWidth = it.measuredWidth

            // Determine if target is in bottom half of screen
            val isNearBottom = bottom > usableHeight * 0.6f

            // Force ABOVE positioning for elements near the bottom
            val effectivePosition = if (isNearBottom && position == Position.BELOW) {
                Position.ABOVE
            } else {
                position
            }

            when (effectivePosition) {
                Position.ABOVE -> {
                    // Position above the target
                    val proposedTop = (top - bubbleHeight - margin * 2).toInt()
                    if (proposedTop > topSafeArea) {
                        params.gravity = android.view.Gravity.NO_GRAVITY
                        params.topMargin = proposedTop
                        val targetCenterX = (left + right) / 2
                        params.leftMargin = (targetCenterX - bubbleWidth / 2).toInt()
                            .coerceAtLeast(margin)
                            .coerceAtMost(screenWidth - bubbleWidth - margin)
                    } else {
                        // If can't fit above, center in available space
                        params.gravity = android.view.Gravity.CENTER_HORIZONTAL
                        params.topMargin = topSafeArea + margin
                        params.leftMargin = margin
                        params.rightMargin = margin
                    }
                }
                Position.BELOW -> {
                    // Position below the target
                    val proposedTop = (bottom + margin).toInt()
                    val proposedBottom = proposedTop + bubbleHeight

                    if (proposedBottom < screenHeight - bottomNavHeight) {
                        params.gravity = android.view.Gravity.NO_GRAVITY
                        params.topMargin = proposedTop
                        val targetCenterX = (left + right) / 2
                        params.leftMargin = (targetCenterX - bubbleWidth / 2).toInt()
                            .coerceAtLeast(margin)
                            .coerceAtMost(screenWidth - bubbleWidth - margin)
                    } else {
                        // Try above instead
                        val aboveTop = (top - bubbleHeight - margin * 2).toInt()
                        if (aboveTop > topSafeArea) {
                            params.gravity = android.view.Gravity.NO_GRAVITY
                            params.topMargin = aboveTop
                            val targetCenterX = (left + right) / 2
                            params.leftMargin = (targetCenterX - bubbleWidth / 2).toInt()
                                .coerceAtLeast(margin)
                                .coerceAtMost(screenWidth - bubbleWidth - margin)
                        } else {
                            // Center in available space
                            params.gravity = android.view.Gravity.CENTER
                            params.topMargin = 0
                            params.leftMargin = margin
                            params.rightMargin = margin
                        }
                    }
                }
                Position.LEFT, Position.RIGHT -> {
                    // For horizontal positioning, ensure it fits vertically
                    params.gravity = android.view.Gravity.NO_GRAVITY
                    params.topMargin = top.toInt()
                        .coerceAtLeast(topSafeArea)
                        .coerceAtMost(screenHeight - bottomNavHeight - bubbleHeight)

                    if (effectivePosition == Position.LEFT) {
                        params.leftMargin = (left - bubbleWidth - margin).toInt()
                            .coerceAtLeast(margin)
                    } else {
                        params.leftMargin = (right + margin).toInt()
                            .coerceAtMost(screenWidth - bubbleWidth - margin)
                    }
                }
            }

            it.layoutParams = params
        }
    }

    private fun nextStep() {
        currentStep++
        showStep(currentStep)
    }

    private fun completeTutorial() {
        // Clean up overlay
        cleanup()

        // Save that tutorial has been completed and clear in-progress state
        prefs.edit()
            .putBoolean("tutorial_completed", true)
            .putBoolean("tutorial_in_progress", false)
            .putInt("tutorial_current_step", 0)
            .apply()

        // Invoke completion callback
        tutorialCallback?.invoke()
    }
}