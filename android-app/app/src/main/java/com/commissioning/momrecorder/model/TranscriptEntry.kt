package com.commissioning.momrecorder.model

data class TranscriptEntry(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFinal: Boolean = false
)

data class RecordingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val entries: MutableList<TranscriptEntry> = mutableListOf(),
    var mom: MomReport? = null
) {
    fun fullTranscript(): String = entries.filter { it.isFinal }.joinToString(" ") { it.text }
    fun duration(): Long = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime
}
