package com.commissioning.momrecorder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.commissioning.momrecorder.R
import com.commissioning.momrecorder.model.MomReport
import java.text.SimpleDateFormat
import java.util.*

class MomListAdapter(
    private val onClick: (MomReport) -> Unit,
    private val onDelete: (MomReport) -> Unit
) : ListAdapter<MomReport, MomListAdapter.VH>(DiffCb()) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvMomTitle)
        val tvDate: TextView = view.findViewById(R.id.tvMomDate)
        val tvSummary: TextView = view.findViewById(R.id.tvMomSummary)
        val tvActions: TextView = view.findViewById(R.id.tvActionCount)
        val btnDelete: View = view.findViewById(R.id.btnDeleteMom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mom, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val mom = getItem(position)
        holder.tvTitle.text = mom.meetingTitle.ifBlank { "Meeting" }
        holder.tvDate.text = mom.date.ifBlank {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(mom.createdAt))
        }
        holder.tvSummary.text = mom.summary.take(120) + if (mom.summary.length > 120) "..." else ""
        val actionCount = mom.actionItems.size
        holder.tvActions.text = "$actionCount action item${if (actionCount != 1) "s" else ""}"
        holder.itemView.setOnClickListener { onClick(mom) }
        holder.btnDelete.setOnClickListener { onDelete(mom) }
    }

    class DiffCb : DiffUtil.ItemCallback<MomReport>() {
        override fun areItemsTheSame(a: MomReport, b: MomReport) = a.id == b.id
        override fun areContentsTheSame(a: MomReport, b: MomReport) = a == b
    }
}
