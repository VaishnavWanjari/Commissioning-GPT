package com.commissioning.momrecorder.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.commissioning.momrecorder.R
import com.commissioning.momrecorder.databinding.ItemTrackerBinding
import com.commissioning.momrecorder.model.ActionStatus
import com.commissioning.momrecorder.model.Priority
import com.commissioning.momrecorder.model.TrackerItem

class TrackerAdapter(
    private val onStatusChanged: (meetingId: String, actionId: String, newStatus: ActionStatus) -> Unit
) : ListAdapter<TrackerItem, TrackerAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TrackerItem>() {
            override fun areItemsTheSame(a: TrackerItem, b: TrackerItem) =
                a.actionItem.id == b.actionItem.id
            override fun areContentsTheSame(a: TrackerItem, b: TrackerItem) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemTrackerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrackerItem) {
            val action = item.actionItem

            binding.tvActionText.text = action.action
            binding.tvMeetingName.text = "📋 ${item.meetingTitle.ifBlank { "Untitled Meeting" }}"
            binding.tvAssignee.text = if (action.assignedTo.isNotBlank()) "👤 ${action.assignedTo}" else ""
            binding.tvDueDate.text = if (action.dueDate.isNotBlank()) "📅 ${action.dueDate}" else ""

            // Priority bar color
            val priorityColor = when (action.priority) {
                Priority.HIGH -> binding.root.context.getColor(R.color.priority_high)
                Priority.MEDIUM -> binding.root.context.getColor(R.color.priority_medium)
                Priority.LOW -> binding.root.context.getColor(R.color.priority_low)
            }
            binding.viewPriorityBar.setBackgroundColor(priorityColor)

            // Strike-through for completed
            if (action.status == ActionStatus.COMPLETED) {
                binding.tvActionText.paintFlags =
                    binding.tvActionText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvActionText.alpha = 0.5f
            } else {
                binding.tvActionText.paintFlags =
                    binding.tvActionText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvActionText.alpha = 1f
            }

            // Status chip
            val (chipText, chipBgColor, chipTextColor) = when (action.status) {
                ActionStatus.PENDING -> Triple(
                    "Pending",
                    binding.root.context.getColor(R.color.priority_medium_bg),
                    binding.root.context.getColor(R.color.priority_medium)
                )
                ActionStatus.IN_PROGRESS -> Triple(
                    "In Progress",
                    binding.root.context.getColor(R.color.primary_container),
                    binding.root.context.getColor(R.color.primary)
                )
                ActionStatus.COMPLETED -> Triple(
                    "Done",
                    binding.root.context.getColor(R.color.priority_low_bg),
                    binding.root.context.getColor(R.color.priority_low)
                )
                ActionStatus.DEFERRED -> Triple(
                    "Deferred",
                    binding.root.context.getColor(R.color.surface_variant),
                    binding.root.context.getColor(R.color.on_surface_variant)
                )
            }
            binding.chipStatus.text = chipText
            binding.chipStatus.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(chipBgColor)
            binding.chipStatus.setTextColor(chipTextColor)

            binding.chipStatus.setOnClickListener {
                val nextStatus = when (action.status) {
                    ActionStatus.PENDING -> ActionStatus.IN_PROGRESS
                    ActionStatus.IN_PROGRESS -> ActionStatus.COMPLETED
                    ActionStatus.COMPLETED -> ActionStatus.PENDING
                    ActionStatus.DEFERRED -> ActionStatus.PENDING
                }
                onStatusChanged(item.meetingId, action.id, nextStatus)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
