package com.example.stepcounterhunting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    private lateinit var totalStepsText: TextView
    private lateinit var animalsCollectedText: TextView
    private lateinit var regionsExploredText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalStepsText = view.findViewById(R.id.total_steps_text)
        animalsCollectedText = view.findViewById(R.id.animals_collected_text)
        regionsExploredText = view.findViewById(R.id.regions_explored_text)

        updateStats()
    }

    private fun updateStats() {
        val stats = DataManager.getStats()
        totalStepsText.text = "Total Steps: ${stats.totalSteps}"
        animalsCollectedText.text = "Animals Collected: ${stats.animalsCollected}"
        regionsExploredText.text = "Regions Explored: ${stats.regionsExplored}"
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }
}