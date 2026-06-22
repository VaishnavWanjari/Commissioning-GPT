package com.commissioning.momrecorder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.commissioning.momrecorder.adapter.ActionItemAdapter
import com.commissioning.momrecorder.databinding.ActivityMomDetailBinding
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.util.PdfExporter
import kotlinx.coroutines.*

class MomDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOM = "extra_mom"
    }

    private lateinit var binding: ActivityMomDetailBinding
    private lateinit var mom: MomReport
    private lateinit var actionAdapter: ActionItemAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMomDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_MOM, MomReport::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_MOM)!!
        }

        title = mom.meetingTitle.ifBlank { "Meeting Minutes" }
        populateMom()
    }

    private fun populateMom() {
        with(binding) {
            tvMeetingTitle.text = mom.meetingTitle.ifBlank { "Meeting Minutes" }
            tvMeetingDate.text = buildString {
                append(mom.date.ifBlank { "" })
                if (mom.time.isNotBlank()) append(" at ${mom.time}")
            }
            tvMeetingDuration.text = "Duration: ${mom.duration.ifBlank { "N/A" }}"
            tvMeetingLocation.text = mom.location.ifBlank { "Video Call" }

            if (mom.attendees.isNotEmpty()) {
                tvAttendeesLabel.visibility = View.VISIBLE
                tvAttendees.visibility = View.VISIBLE
                tvAttendees.text = mom.attendees.joinToString(" • ")
            }
            if (mom.facilitator.isNotBlank()) {
                tvFacilitator.visibility = View.VISIBLE
                tvFacilitator.text = "Facilitated by: ${mom.facilitator}"
            }

            tvSummary.text = mom.summary.ifBlank { "No summary available." }

            if (mom.keyDecisions.isNotEmpty()) {
                cardDecisions.visibility = View.VISIBLE
                tvDecisions.text = mom.keyDecisions.mapIndexed { i, d ->
                    "${i + 1}. ${d.decision}${if (d.context.isNotBlank()) "\n   ${d.context}" else ""}"
                }.joinToString("\n\n")
            }

            actionAdapter = ActionItemAdapter()
            rvActionItems.apply {
                layoutManager = LinearLayoutManager(this@MomDetailActivity)
                adapter = actionAdapter
                isNestedScrollingEnabled = false
            }
            actionAdapter.submitList(mom.actionItems)
            tvActionCount.text = "${mom.actionItems.size} Action Items"

            if (mom.discussionPoints.isNotEmpty()) {
                cardDiscussion.visibility = View.VISIBLE
                tvDiscussionPoints.text = mom.discussionPoints.joinToString("\n") { "• $it" }
            }

            if (mom.nextSteps.isNotEmpty()) {
                cardNextSteps.visibility = View.VISIBLE
                tvNextSteps.text = mom.nextSteps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
            }

            if (mom.nextMeetingDate.isNotBlank()) {
                cardNextMeeting.visibility = View.VISIBLE
                tvNextMeetingDate.text = mom.nextMeetingDate
            }

            if (mom.remarks.isNotBlank()) {
                cardRemarks.visibility = View.VISIBLE
                tvRemarks.text = mom.remarks
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.mom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_export_pdf -> { exportPdf(); true }
            R.id.action_share -> { shareMomText(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportPdf() {
        scope.launch {
            val file = withContext(Dispatchers.IO) { PdfExporter.exportToPdf(this@MomDetailActivity, mom) }
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    this@MomDetailActivity,
                    "${packageName}.fileprovider",
                    file
                )
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Open PDF with..."
                ))
            } else {
                Toast.makeText(this@MomDetailActivity, "PDF export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareMomText() {
        val text = buildString {
            appendLine("MINUTES OF MEETING")
            appendLine("=".repeat(50))
            appendLine("Meeting: ${mom.meetingTitle.ifBlank { "Meeting" }}")
            appendLine("Date: ${mom.date} ${mom.time}".trim())
            appendLine("Duration: ${mom.duration}")
            appendLine("Platform: ${mom.location}")
            if (mom.attendees.isNotEmpty()) appendLine("Attendees: ${mom.attendees.joinToString(", ")}")
            appendLine()
            appendLine("SUMMARY")
            appendLine("-".repeat(30))
            appendLine(mom.summary)
            if (mom.keyDecisions.isNotEmpty()) {
                appendLine()
                appendLine("KEY DECISIONS")
                appendLine("-".repeat(30))
                mom.keyDecisions.forEachIndexed { i, d ->
                    appendLine("${i + 1}. ${d.decision}")
                    if (d.context.isNotBlank()) appendLine("   Context: ${d.context}")
                }
            }
            if (mom.actionItems.isNotEmpty()) {
                appendLine()
                appendLine("ACTION ITEMS")
                appendLine("-".repeat(30))
                mom.actionItems.forEachIndexed { i, a ->
                    appendLine("${i + 1}. ${a.action}")
                    appendLine("   Assigned To: ${a.assignedTo.ifBlank { "TBD" }}")
                    appendLine("   Due Date: ${a.dueDate.ifBlank { "TBD" }}")
                    appendLine("   Priority: ${a.priority}")
                    if (a.remarks.isNotBlank()) appendLine("   Remarks: ${a.remarks}")
                }
            }
            if (mom.nextSteps.isNotEmpty()) {
                appendLine()
                appendLine("NEXT STEPS")
                appendLine("-".repeat(30))
                mom.nextSteps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
            }
            if (mom.nextMeetingDate.isNotBlank()) {
                appendLine()
                appendLine("Next Meeting: ${mom.nextMeetingDate}")
            }
            if (mom.remarks.isNotBlank()) {
                appendLine()
                appendLine("REMARKS")
                appendLine("-".repeat(30))
                appendLine(mom.remarks)
            }
        }
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "MOM: ${mom.meetingTitle.ifBlank { "Meeting" }}")
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share MOM via..."
        ))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
