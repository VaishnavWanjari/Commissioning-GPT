package com.commissioning.momrecorder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.commissioning.momrecorder.R
import com.commissioning.momrecorder.model.TranscriptEntry

class TranscriptAdapter : ListAdapter<TranscriptEntry, TranscriptAdapter.VH>(DiffCb()) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvTranscriptText)
        val indicator: View = view.findViewById(R.id.viewIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transcript, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        holder.text.text = entry.text
        holder.text.alpha = if (entry.isFinal) 1f else 0.6f
        holder.indicator.visibility = if (!entry.isFinal) View.VISIBLE else View.GONE
    }

    class DiffCb : DiffUtil.ItemCallback<TranscriptEntry>() {
        override fun areItemsTheSame(a: TranscriptEntry, b: TranscriptEntry) =
            a.timestamp == b.timestamp && a.isFinal == b.isFinal
        override fun areContentsTheSame(a: TranscriptEntry, b: TranscriptEntry) = a == b
    }
}
