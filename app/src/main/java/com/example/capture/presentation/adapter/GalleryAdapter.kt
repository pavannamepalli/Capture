
package com.example.capture.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.capture.R
import java.io.File

class GalleryAdapter : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
    
    private var imageFiles: List<File> = emptyList()
    

    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return GalleryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(imageFiles[position])
    }
    
    override fun getItemCount(): Int = imageFiles.size
    
    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.gallery_image)
        
        fun bind(file: File) {
            Glide.with(itemView.context)
                .load(file)
                .centerCrop()
                .into(imageView)
        }
    }
}
