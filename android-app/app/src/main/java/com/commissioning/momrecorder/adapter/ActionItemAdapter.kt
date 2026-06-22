package com.commissioning.momrecorder.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.commissioning.momrecorder.R
import com.commissioning.momrecorder.model.ActionItem
import com.commissioning.momrecorder.model.Priority

class ActionItemAdapter : ListAdapter<ActionItem, ActionItemAdapter.VH>(DiffCb()) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAction: TextView = view.findViewById(R.id.tvActionText)
        val tvAssigned: TextView = view.findViewById(R.id.tvAssignedTo)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvPriority: TextView = view.findViewById(R.id.tvPriority)
        val tvRemarks: TextView = view.findViewById(R.id.tvActionRemarks)
        val priorityBar: View = view.findViewById(R.id.viewPriorityBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvAction.text = item.action
        holder.tvAssigned.text = "Assigned to: ${item.assignedTo.ifBlank { "TBD" }}"
        holder.tvDueDate.text = if (item.dueDate.isNotBlank()) "Due: ${item.dueDate}" else "Due: Not set"
        holder.tvPriority.text = item.priority.name

        val (color, bg) = when (item.priority) {
            Priority.HIGH -> Color.parseColor("#C62828") to Color.parseColor("#FFEBEE")
            Priority.LOW -> Color.parseColor("#2E7D32") to Color.parseColor("#E8F5E9")
            Priority.MEDIUM -> Color.parseColor("#E65100") to Color.parseColor("#FFF3E0")
        }
        holder.tvPriority.setTextColor(color)
        holder.tvPriority.setBackgroundColor(bg)
        holder.priorityBar.setBackgroundColor(color)

        if (item.remarks.isNotBlank()) {
            holder.tvRemarks.visibility = View.VISIBLE
            holder.tvRemarks.text = item.remarks
        } else {
            holder.tvRemarks.visibility = View.GONE
        }
    }

    class DiffCb : DiffUtil.ItemCallback<ActionItem>() {
        override fun areItemsTheSame(a: ActionItem, b: ActionItem) = a.id == b.id
        override fun areContentsTheSame(a: ActionItem, b: ActionItem) = a == b
    }
}
