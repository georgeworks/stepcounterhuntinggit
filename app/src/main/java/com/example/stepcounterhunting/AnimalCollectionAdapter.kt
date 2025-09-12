package com.example.stepcounterhunting

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat

class AnimalCollectionAdapter(
    private var items: List<CollectionItem>,
    private val onAnimalClick: (CollectionItem.AnimalCard) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var currentToast: android.widget.Toast? = null

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ANIMAL = 1
    }

    // ViewHolder for region headers
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val regionName: TextView = view.findViewById(R.id.region_name)
        val caughtCount: TextView = view.findViewById(R.id.caught_count)
    }

    // ViewHolder for animal cards
    class AnimalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.animal_card)
        val borderFrame: FrameLayout = view.findViewById(R.id.border_frame)
        val imageView: ImageView = view.findViewById(R.id.animal_image)
        val nameText: TextView = view.findViewById(R.id.animal_name)
        val rarityText: TextView = view.findViewById(R.id.animal_rarity)
        val regionText: TextView = view.findViewById(R.id.animal_region)
        val questionMark: TextView = view.findViewById(R.id.question_mark)
        val hintText: TextView = view.findViewById(R.id.hint_text)
        val starIndicator: ImageView = view.findViewById(R.id.star_indicator)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CollectionItem.Header -> VIEW_TYPE_HEADER
            is CollectionItem.AnimalCard -> VIEW_TYPE_ANIMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_collection_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_animal_collection, parent, false)
                AnimalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CollectionItem.Header -> {
                (holder as HeaderViewHolder).apply {
                    regionName.text = item.regionName
                    val totalInRegion = countAnimalsInRegion(item.regionName)
                    caughtCount.text = "${item.caughtCount} / $totalInRegion caught"
                }
            }
            is CollectionItem.AnimalCard -> {
                (holder as AnimalViewHolder).apply {
                    if (item.isCaught) {
                        // Caught animal - show normally
                        nameText.text = item.animal.name
                        rarityText.text = item.animal.rarity.displayName
                        regionText.text = item.animal.region

                        // Set rarity color
                        cardView.setCardBackgroundColor(Color.parseColor(item.animal.rarity.color))

                        // Clear any border from previously uncaught state
                        borderFrame.background = null

                        // Show image - special case for Hellbender
                        when (item.animal.id) {
                            "app_6" -> imageView.setImageResource(R.drawable.hellbendercard)
                            "app_1" -> imageView.setImageResource(R.drawable.brooktroutcard)
                            else -> imageView.setImageResource(item.animal.imageResource)
                        }
                        imageView.colorFilter = null
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                        // Show star based on count
                        if (item.count >= 3) {
                            starIndicator.visibility = View.VISIBLE
                            starIndicator.setImageResource(R.drawable.ic_star_filled)
                        } else {
                            starIndicator.visibility = View.VISIBLE
                            starIndicator.setImageResource(R.drawable.ic_star_empty)
                        }

                        // Add star click listener
                        starIndicator.setOnClickListener {
                            val message = if (item.count >= 3) {
                                "✨ Triplicate complete! You've caught ${item.count} ${item.animal.name}s"
                            } else {
                                "Catch 3 duplicates to complete! Currently: ${item.count}/3"
                            }

                            currentToast?.cancel()

                            currentToast = android.widget.Toast.makeText(
                                holder.itemView.context,
                                message,
                                android.widget.Toast.LENGTH_SHORT
                            )
                            currentToast?.show()
                        }

                        // Hide question mark and show hint text
                        questionMark.visibility = View.GONE
                        hintText.visibility = View.VISIBLE
                        hintText.text = "Tap to view details"

                        // Enable click
                        cardView.isClickable = true
                        cardView.isFocusable = true
                        cardView.setOnClickListener {
                            onAnimalClick(item)
                        }

                    } else {
                        // Uncaught animal - show as mystery
                        nameText.text = "???"
                        rarityText.text = item.animal.rarity.displayName
                        regionText.text = item.animal.region

                        // Hide star for uncaught animals
                        starIndicator.visibility = View.GONE
                        starIndicator.setOnClickListener(null)

                        // Gray background with border effect
                        cardView.setCardBackgroundColor(Color.parseColor("#CCCCCC"))

                        // Add a border by setting padding and background to the border frame
                        val borderDrawable = GradientDrawable().apply {
                            setColor(Color.parseColor("#CCCCCC"))
                            setStroke(6, Color.parseColor("#666666"))
                            cornerRadius = 16f
                        }
                        borderFrame.background = borderDrawable

                        // Show image as grayscale silhouette
                        imageView.setImageResource(item.animal.imageResource)
                        val matrix = ColorMatrix()
                        matrix.setSaturation(0f) // Grayscale
                        matrix.setScale(0.3f, 0.3f, 0.3f, 1f) // Darken
                        imageView.colorFilter = ColorMatrixColorFilter(matrix)

                        // Show question mark
                        questionMark.visibility = View.VISIBLE
                        hintText.visibility = View.VISIBLE
                        hintText.text = "Not caught yet"

                        // Disable click or show hint
                        cardView.isClickable = false
                        cardView.isFocusable = false
                        cardView.setOnClickListener(null)
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun isHeader(position: Int): Boolean {
        return items[position] is CollectionItem.Header
    }

    private fun countAnimalsInRegion(regionName: String): Int {
        // Count total animals in each region for progress display
        return when (regionName) {
            "Northeast", "Southeast", "Midwest", "West" -> 10
            else -> items.count {
                it is CollectionItem.AnimalCard && it.animal.region == regionName
            }
        }
    }

    fun updateData(newItems: List<CollectionItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}