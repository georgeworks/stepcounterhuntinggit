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

        adapter = AnimalCollectionAdapter(DataManager.getCollection())
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(DataManager.getCollection())
    }
}