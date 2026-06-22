package com.commissioning.momrecorder.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.util.Log
import com.commissioning.momrecorder.model.ActionItem
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.model.Priority
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val TAG = "PdfExporter"
    private const val PAGE_WIDTH = 595   // A4 width in points
    private const val PAGE_HEIGHT = 842  // A4 height in points
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 18f

    fun exportToPdf(context: Context, report: MomReport): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPos = MARGIN

            val writer = PageWriter(document, canvas, page, yPos)
            writer.drawMom(report)

            document.finishPage(writer.currentPage)
            val file = File(context.getExternalFilesDir(null), "MOM_${report.id}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "PDF export failed", e)
            null
        }
    }

    private class PageWriter(
        private val document: PdfDocument,
        var canvas: Canvas,
        var currentPage: PdfDocument.Page,
        private var y: Float
    ) {
        private val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val headingPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        private val labelPaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val dividerPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        private val highPaint = Paint().apply { color = Color.parseColor("#C62828"); textSize = 10f; isAntiAlias = true }
        private val medPaint = Paint().apply { color = Color.parseColor("#E65100"); textSize = 10f; isAntiAlias = true }
        private val lowPaint = Paint().apply { color = Color.parseColor("#2E7D32"); textSize = 10f; isAntiAlias = true }

        fun drawMom(r: MomReport) {
            // Header band
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 80f, Paint().apply { color = Color.parseColor("#1565C0") })
            canvas.drawText("MINUTES OF MEETING", MARGIN, 35f, Paint().apply {
                color = Color.WHITE; textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            })
            canvas.drawText("Shefali • AI-Powered Meeting Assistant", MARGIN, 58f, Paint().apply {
                color = Color.parseColor("#BBDEFB"); textSize = 11f; isAntiAlias = true
            })

            y = 100f

            // Meeting info box
            drawBox(MARGIN, y, PAGE_WIDTH - MARGIN, y + 72f, Color.parseColor("#E3F2FD"))
            y += 10f
            drawLabelValue("Meeting Title:", r.meetingTitle.ifBlank { "Meeting" })
            drawLabelValue("Date:", "${r.date} ${r.time}".trim())
            drawLabelValue("Duration:", r.duration.ifBlank { "N/A" })
            drawLabelValue("Platform:", r.location.ifBlank { "Video Call" })
            y += 8f

            if (r.attendees.isNotEmpty()) {
                drawLabelValue("Attendees:", r.attendees.joinToString(" • "))
            }
            if (r.facilitator.isNotBlank()) {
                drawLabelValue("Facilitator:", r.facilitator)
            }

            spacer(16f)
            drawSection("EXECUTIVE SUMMARY")
            drawWrappedText(r.summary, bodyPaint)

            if (r.keyDecisions.isNotEmpty()) {
                spacer(12f)
                drawSection("KEY DECISIONS")
                r.keyDecisions.forEachIndexed { i, d ->
                    checkNewPage()
                    canvas.drawText("${i + 1}. ${d.decision}", MARGIN + 8f, y, bodyPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                    y += LINE_HEIGHT
                    if (d.context.isNotBlank()) {
                        canvas.drawText("   ${d.context}", MARGIN + 16f, y, bodyPaint.apply { typeface = Typeface.DEFAULT })
                        y += LINE_HEIGHT
                    }
                }
            }

            if (r.actionItems.isNotEmpty()) {
                spacer(12f)
                drawSection("ACTION ITEMS")
                drawActionItemsTable(r.actionItems)
            }

            if (r.discussionPoints.isNotEmpty()) {
                spacer(12f)
                drawSection("DISCUSSION POINTS")
                r.discussionPoints.forEach { pt ->
                    checkNewPage()
                    canvas.drawText("• $pt", MARGIN + 8f, y, bodyPaint)
                    y += LINE_HEIGHT
                }
            }

            if (r.nextSteps.isNotEmpty()) {
                spacer(12f)
                drawSection("NEXT STEPS")
                r.nextSteps.forEachIndexed { i, step ->
                    checkNewPage()
                    canvas.drawText("${i + 1}. $step", MARGIN + 8f, y, bodyPaint)
                    y += LINE_HEIGHT
                }
            }

            if (r.nextMeetingDate.isNotBlank()) {
                spacer(12f)
                drawSection("NEXT MEETING")
                canvas.drawText(r.nextMeetingDate, MARGIN + 8f, y, bodyPaint)
                y += LINE_HEIGHT
            }

            if (r.remarks.isNotBlank()) {
                spacer(12f)
                drawSection("REMARKS")
                drawWrappedText(r.remarks, bodyPaint)
            }

            // Footer
            spacer(20f)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += 10f
            canvas.drawText(
                "Generated by Shefali • ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                MARGIN, y, Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true }
            )
        }

        private fun drawActionItemsTable(items: List<ActionItem>) {
            val colWidths = floatArrayOf(200f, 100f, 90f, 55f, 50f)
            val headers = arrayOf("Action", "Assigned To", "Due Date", "Priority", "Status")
            val rowH = 20f
            val startX = MARGIN

            // Table header
            var xPos = startX
            canvas.drawRect(startX, y, PAGE_WIDTH - MARGIN, y + rowH, Paint().apply { color = Color.parseColor("#1565C0") })
            headers.forEachIndexed { i, h ->
                canvas.drawText(h, xPos + 4f, y + 14f, Paint().apply {
                    color = Color.WHITE; textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
                })
                xPos += colWidths[i]
            }
            y += rowH

            // Rows
            items.forEachIndexed { idx, item ->
                checkNewPage()
                val rowBg = if (idx % 2 == 0) Color.parseColor("#F5F5F5") else Color.WHITE
                canvas.drawRect(startX, y, PAGE_WIDTH - MARGIN, y + rowH, Paint().apply { color = rowBg })
                xPos = startX
                val priorityPaint = when (item.priority) { Priority.HIGH -> highPaint; Priority.LOW -> lowPaint; else -> medPaint }
                val cells = listOf(item.action, item.assignedTo, item.dueDate, item.priority.name, item.status.name)
                cells.forEachIndexed { ci, text ->
                    val cellPaint = if (ci == 3) priorityPaint else bodyPaint
                    canvas.drawText(text.take(30), xPos + 4f, y + 14f, cellPaint)
                    xPos += colWidths[ci]
                }
                y += rowH
            }
        }

        private fun drawLabelValue(label: String, value: String) {
            checkNewPage()
            canvas.drawText(label, MARGIN + 8f, y, labelPaint)
            canvas.drawText(value, MARGIN + 110f, y, bodyPaint)
            y += LINE_HEIGHT
        }

        private fun drawSection(title: String) {
            checkNewPage()
            canvas.drawRect(MARGIN, y - 2f, PAGE_WIDTH - MARGIN, y + 16f, Paint().apply { color = Color.parseColor("#1565C0") })
            canvas.drawText(title, MARGIN + 8f, y + 12f, Paint().apply {
                color = Color.WHITE; textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            })
            y += 24f
        }

        private fun drawWrappedText(text: String, paint: Paint) {
            val maxWidth = (PAGE_WIDTH - MARGIN * 2 - 8f).toInt()
            val words = text.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > maxWidth) {
                    checkNewPage()
                    canvas.drawText(line.toString(), MARGIN + 8f, y, paint)
                    y += LINE_HEIGHT
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(test)
                }
            }
            if (line.isNotEmpty()) {
                checkNewPage()
                canvas.drawText(line.toString(), MARGIN + 8f, y, paint)
                y += LINE_HEIGHT
            }
        }

        private fun drawBox(left: Float, top: Float, right: Float, bottom: Float, fillColor: Int) {
            canvas.drawRect(left, top, right, bottom, Paint().apply { color = fillColor })
        }

        private fun spacer(sp: Float) { y += sp }

        private fun checkNewPage() {
            if (y > PAGE_HEIGHT - 60f) {
                document.finishPage(currentPage)
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                currentPage = document.startPage(pageInfo)
                canvas = currentPage.canvas
                y = MARGIN
            }
        }
    }
}
