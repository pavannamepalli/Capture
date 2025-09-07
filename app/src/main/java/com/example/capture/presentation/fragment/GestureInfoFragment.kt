package com.example.capture.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capture.R
import com.example.capture.databinding.FragmentGestureInfoBinding
import com.example.capture.presentation.adapter.GestureInfoAdapter
import com.example.capture.presentation.model.GestureInfo

class GestureInfoFragment : Fragment() {

    private var _binding: FragmentGestureInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestureInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val gestureList = getGestureInfoList()
        val adapter = GestureInfoAdapter(gestureList)
        
        binding.recyclerViewGestures.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_gesture_info_to_camera)
            } catch (e: Exception) {
                                findNavController().navigate(R.id.camera_fragment)
            }
        }
        
        binding.btnStartCamera.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_gesture_info_to_camera)
            } catch (e: Exception) {
                                findNavController().navigate(R.id.camera_fragment)
            }
        }
    }

    private fun getGestureInfoList(): List<GestureInfo> {
        return listOf(
            GestureInfo(
                icon = "✋",
                name = getString(R.string.gesture_palm_name),
                description = getString(R.string.gesture_palm_description),
                cooldown = getString(R.string.cooldown_2_seconds),
                instruction = getString(R.string.gesture_palm_instruction)
            ),
            GestureInfo(
                icon = "✌️",
                name = getString(R.string.gesture_peace_sign_name),
                description = getString(R.string.gesture_peace_sign_start_description),
                cooldown = getString(R.string.cooldown_3_seconds),
                instruction = getString(R.string.gesture_peace_sign_instruction)
            ),
            GestureInfo(
                icon = "✌️",
                name = getString(R.string.gesture_peace_sign_name),
                description = getString(R.string.gesture_peace_sign_stop_description),
                cooldown = getString(R.string.cooldown_3_seconds),
                instruction = getString(R.string.gesture_peace_sign_stop_instruction)
            ),
            GestureInfo(
                icon = "👍",
                name = getString(R.string.gesture_thumbs_up_name),
                description = getString(R.string.gesture_thumbs_up_description),
                cooldown = getString(R.string.cooldown_2_seconds),
                instruction = getString(R.string.gesture_thumbs_up_instruction)
            ),
            GestureInfo(
                icon = "👌",
                name = getString(R.string.gesture_ok_sign_name),
                description = getString(R.string.gesture_ok_sign_description),
                cooldown = getString(R.string.cooldown_2_seconds),
                instruction = getString(R.string.gesture_ok_sign_instruction)
            ),
            GestureInfo(
                icon = "🤏",
                name = getString(R.string.gesture_pinch_zoom_in_name),
                description = getString(R.string.gesture_pinch_zoom_in_description),
                cooldown = getString(R.string.cooldown_1_second),
                instruction = getString(R.string.gesture_pinch_zoom_in_instruction)
            ),
            GestureInfo(
                icon = "🤏",
                name = getString(R.string.gesture_pinch_zoom_out_name),
                description = getString(R.string.gesture_pinch_zoom_out_description),
                cooldown = getString(R.string.cooldown_1_second),
                instruction = getString(R.string.gesture_pinch_zoom_out_instruction)
            ),
            GestureInfo(
                icon = "🤟",
                name = getString(R.string.gesture_three_fingers_name),
                description = getString(R.string.gesture_three_fingers_description),
                cooldown = getString(R.string.cooldown_2_seconds),
                instruction = getString(R.string.gesture_three_fingers_instruction)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
