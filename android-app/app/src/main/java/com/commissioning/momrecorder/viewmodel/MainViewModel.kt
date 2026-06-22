package com.commissioning.momrecorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.commissioning.momrecorder.api.ClaudeApiClient
import com.commissioning.momrecorder.api.MeetingContext
import com.commissioning.momrecorder.api.MomParser
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.model.RecordingSession
import com.commissioning.momrecorder.model.TranscriptEntry
import com.commissioning.momrecorder.util.MomStorage
import com.commissioning.momrecorder.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private var currentSession: RecordingSession? = null

    private val _transcriptEntries = MutableLiveData<List<TranscriptEntry>>(emptyList())
    val transcriptEntries: LiveData<List<TranscriptEntry>> = _transcriptEntries

    private val _fullTranscript = MutableLiveData("")
    val fullTranscript: LiveData<String> = _fullTranscript

    private val _recordingState = MutableLiveData(RecordingState.IDLE)
    val recordingState: LiveData<RecordingState> = _recordingState

    private val _duration = MutableLiveData(0L)
    val duration: LiveData<Long> = _duration

    private val _momGenerating = MutableLiveData(false)
    val momGenerating: LiveData<Boolean> = _momGenerating

    private val _generatedMom = MutableLiveData<MomReport?>()
    val generatedMom: LiveData<MomReport?> = _generatedMom

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _savedMoms = MutableLiveData<List<MomReport>>(emptyList())
    val savedMoms: LiveData<List<MomReport>> = _savedMoms

    init {
        loadSavedMoms()
    }

    fun startSession(title: String = "") {
        currentSession = RecordingSession(title = title)
        _transcriptEntries.value = emptyList()
        _fullTranscript.value = ""
        _recordingState.value = RecordingState.RECORDING
    }

    fun appendTranscript(text: String, isFinal: Boolean) {
        val entry = TranscriptEntry(text = text, isFinal = isFinal)
        currentSession?.entries?.add(entry)
        val current = _transcriptEntries.value?.toMutableList() ?: mutableListOf()
        if (!isFinal) {
            // Replace last partial with new partial
            if (current.isNotEmpty() && !current.last().isFinal) {
                current[current.lastIndex] = entry
            } else {
                current.add(entry)
            }
        } else {
            // Remove any trailing partial, add final
            if (current.isNotEmpty() && !current.last().isFinal) {
                current[current.lastIndex] = entry
            } else {
                current.add(entry)
            }
        }
        _transcriptEntries.value = current
        _fullTranscript.value = currentSession?.fullTranscript() ?: ""
    }

    fun updateDuration(millis: Long) {
        _duration.value = millis
    }

    fun setPaused() { _recordingState.value = RecordingState.PAUSED }
    fun setResumed() { _recordingState.value = RecordingState.RECORDING }

    fun stopSession() {
        currentSession?.let { it.copy(endTime = System.currentTimeMillis()) }
        _recordingState.value = RecordingState.STOPPED
    }

    fun generateMom(meetingTitle: String = "", platform: String = "Video Call") {
        val transcript = _fullTranscript.value ?: ""
        if (transcript.isBlank()) {
            _error.value = "No transcript available. Please record a meeting first."
            return
        }
        val apiKey = prefs.claudeApiKey
        if (apiKey.isBlank()) {
            _error.value = "Claude API key not configured. Please go to Settings."
            return
        }

        _momGenerating.value = true
        viewModelScope.launch {
            try {
                val client = ClaudeApiClient(apiKey)
                val context = MeetingContext(
                    title = meetingTitle,
                    platform = platform.ifBlank { prefs.defaultMeetingPlatform }
                )
                val jsonResponse = withContext(Dispatchers.IO) {
                    client.generateMom(transcript, context)
                }
                val report = MomParser.parse(jsonResponse, transcript)
                _generatedMom.value = report
                currentSession?.mom = report
                withContext(Dispatchers.IO) {
                    MomStorage.saveMom(getApplication(), report)
                }
                loadSavedMoms()
            } catch (e: Exception) {
                _error.value = "Failed to generate MOM: ${e.message}"
            } finally {
                _momGenerating.value = false
            }
        }
    }

    fun loadSavedMoms() {
        viewModelScope.launch(Dispatchers.IO) {
            val moms = MomStorage.loadAll(getApplication())
            withContext(Dispatchers.Main) {
                _savedMoms.value = moms
            }
        }
    }

    fun deleteMom(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            MomStorage.delete(getApplication(), id)
            loadSavedMoms()
        }
    }

    fun clearError() { _error.value = null }

    enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }
}
