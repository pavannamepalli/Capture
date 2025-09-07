package com.example.capture.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.capture.databinding.ItemGestureInfoBinding
import com.example.capture.presentation.model.GestureInfo

class GestureInfoAdapter(
    private val gestureList: List<GestureInfo>
) : RecyclerView.Adapter<GestureInfoAdapter.GestureInfoViewHolder>() {

    class GestureInfoViewHolder(
        private val binding: ItemGestureInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gesture: GestureInfo) {
            binding.apply {
                textIcon.text = gesture.icon
                textName.text = gesture.name
                textDescription.text = gesture.description
                textCooldown.text = "Cooldown: ${gesture.cooldown}"
                textInstruction.text = gesture.instruction
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureInfoViewHolder {
        val binding = ItemGestureInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GestureInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureInfoViewHolder, position: Int) {
        holder.bind(gestureList[position])
    }

    override fun getItemCount(): Int = gestureList.size
}
