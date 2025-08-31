package com.example.stepcounterhunting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CollectionFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnimalCollectionAdapter

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
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // Pass click listener to adapter
        adapter = AnimalCollectionAdapter(DataManager.getCollection()) { animal ->
            showAnimalDetails(animal)
        }
        recyclerView.adapter = adapter
    }

    private fun showAnimalDetails(animal: Animal) {
        // Create a new dialog instance for viewing collection animals
        val dialog = AnimalDetailDialog(animal)
        dialog.show(childFragmentManager, "animal_detail")
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(DataManager.getCollection())
    }
}