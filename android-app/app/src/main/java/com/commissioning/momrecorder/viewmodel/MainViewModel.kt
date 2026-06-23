package com.commissioning.momrecorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.commissioning.momrecorder.api.ClaudeApiClient
import com.commissioning.momrecorder.api.MeetingContext
import com.commissioning.momrecorder.api.MomParser
import com.commissioning.momrecorder.model.ActionStatus
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.model.RecordingSession
import com.commissioning.momrecorder.model.TrackerItem
import com.commissioning.momrecorder.model.TranscriptEntry
import com.commissioning.momrecorder.util.MomStorage
import com.commissioning.momrecorder.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PreferencesManager(application)
    private var currentSession: RecordingSession? = null

    // Recording state
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Transcript
    private val _transcriptEntries = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcriptEntries: StateFlow<List<TranscriptEntry>> = _transcriptEntries.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    // MOM generation
    private val _momGenerating = MutableStateFlow(false)
    val momGenerating: StateFlow<Boolean> = _momGenerating.asStateFlow()

    // One-shot events
    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    private val _momGenerated = MutableSharedFlow<MomReport>()
    val momGenerated: SharedFlow<MomReport> = _momGenerated.asSharedFlow()

    // History
    private val _savedMoms = MutableStateFlow<List<MomReport>>(emptyList())
    val savedMoms: StateFlow<List<MomReport>> = _savedMoms.asStateFlow()

    // Tracker
    private val _trackerItems = MutableStateFlow<List<TrackerItem>>(emptyList())
    val trackerItems: StateFlow<List<TrackerItem>> = _trackerItems.asStateFlow()

    private var currentTrackerFilter = "ALL"

    init {
        loadSavedMoms()
    }

    // ===================== SESSION =====================

    fun startSession(title: String = "") {
        currentSession = RecordingSession(title = title)
        _transcriptEntries.value = emptyList()
        _fullTranscript.value = ""
        _recordingState.value = RecordingState.RECORDING
    }

    fun appendTranscript(text: String, isFinal: Boolean) {
        val entry = TranscriptEntry(text = text, isFinal = isFinal)
        currentSession?.entries?.add(entry)
        _transcriptEntries.update { current ->
            val list = current.toMutableList()
            if (!isFinal) {
                if (list.isNotEmpty() && !list.last().isFinal) list[list.lastIndex] = entry
                else list.add(entry)
            } else {
                if (list.isNotEmpty() && !list.last().isFinal) list[list.lastIndex] = entry
                else list.add(entry)
            }
            list
        }
        _fullTranscript.value = currentSession?.fullTranscript() ?: ""
    }

    fun updateDuration(millis: Long) { _duration.value = millis }
    fun setPaused() { _recordingState.value = RecordingState.PAUSED }
    fun setResumed() { _recordingState.value = RecordingState.RECORDING }
    fun stopSession() { _recordingState.value = RecordingState.STOPPED }
    fun resetToIdle() { _recordingState.value = RecordingState.IDLE }

    // ===================== MOM GENERATION =====================

    fun generateMom(meetingTitle: String = "", platform: String = "Video Call") {
        val transcript = _fullTranscript.value
        if (transcript.isBlank()) {
            emitSnackbar("No transcript available. Record a meeting first.")
            return
        }
        val apiKey = prefs.claudeApiKey
        if (apiKey.isBlank()) {
            emitSnackbar("Claude API key not set. Go to Settings.")
            return
        }
        _momGenerating.value = true
        viewModelScope.launch {
            try {
                val client = ClaudeApiClient(apiKey)
                val ctx = MeetingContext(
                    title = meetingTitle,
                    platform = platform.ifBlank { prefs.defaultMeetingPlatform }
                )
                val json = withContext(Dispatchers.IO) { client.generateMom(transcript, ctx) }
                val report = MomParser.parse(json, transcript)
                withContext(Dispatchers.IO) { MomStorage.saveMom(getApplication(), report) }
                loadSavedMoms()
                _momGenerated.emit(report)
            } catch (e: Exception) {
                emitSnackbar("Failed to generate MOM: ${e.message}")
            } finally {
                _momGenerating.value = false
            }
        }
    }

    // ===================== HISTORY =====================

    fun loadSavedMoms() {
        viewModelScope.launch(Dispatchers.IO) {
            val moms = MomStorage.loadAll(getApplication())
            withContext(Dispatchers.Main) { _savedMoms.value = moms }
        }
    }

    fun deleteMom(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            MomStorage.delete(getApplication(), id)
            loadSavedMoms()
        }
    }

    fun getMomById(id: String): MomReport? = _savedMoms.value.find { it.id == id }

    // ===================== TRACKER =====================

    fun loadTrackerItems(filter: String = currentTrackerFilter) {
        currentTrackerFilter = filter
        viewModelScope.launch(Dispatchers.IO) {
            val moms = MomStorage.loadAll(getApplication())
            val items = moms.flatMap { mom ->
                mom.actionItems.map { action ->
                    TrackerItem(action, mom.meetingTitle, mom.id, mom.date)
                }
            }.filter { item ->
                when (filter) {
                    "PENDING" -> item.actionItem.status == ActionStatus.PENDING
                    "IN_PROGRESS" -> item.actionItem.status == ActionStatus.IN_PROGRESS
                    "COMPLETED" -> item.actionItem.status == ActionStatus.COMPLETED
                    else -> true
                }
            }.sortedWith(compareBy({ it.actionItem.status.ordinal }, { it.actionItem.priority.ordinal }))
            withContext(Dispatchers.Main) { _trackerItems.value = items }
        }
    }

    fun updateActionStatus(meetingId: String, actionId: String, newStatus: ActionStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            MomStorage.updateActionStatus(getApplication(), meetingId, actionId, newStatus)
            withContext(Dispatchers.Main) { loadTrackerItems(currentTrackerFilter) }
        }
    }

    // ===================== HELPERS =====================

    suspend fun validateApiKey(key: String): Boolean = withContext(Dispatchers.IO) {
        ClaudeApiClient(key).validateApiKey(key)
    }

    private fun emitSnackbar(msg: String) {
        viewModelScope.launch { _snackbar.emit(msg) }
    }

    enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }
}
