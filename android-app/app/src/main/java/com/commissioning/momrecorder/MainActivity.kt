package com.commissioning.momrecorder

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.commissioning.momrecorder.adapter.MomListAdapter
import com.commissioning.momrecorder.adapter.TrackerAdapter
import com.commissioning.momrecorder.adapter.TranscriptAdapter
import com.commissioning.momrecorder.databinding.ActivityMainBinding
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.service.RecordingService
import com.commissioning.momrecorder.util.PreferencesManager
import com.commissioning.momrecorder.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var transcriptAdapter: TranscriptAdapter
    private lateinit var momListAdapter: MomListAdapter
    private lateinit var trackerAdapter: TrackerAdapter
    private lateinit var prefs: PreferencesManager

    private var waveAnimatorSet: AnimatorSet? = null
    private var pulseAnimator: ObjectAnimator? = null

    private val transcriptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RecordingService.BROADCAST_TRANSCRIPT -> {
                    val text = intent.getStringExtra(RecordingService.EXTRA_TEXT) ?: return
                    val isFinal = intent.getBooleanExtra(RecordingService.EXTRA_IS_FINAL, false)
                    viewModel.appendTranscript(text, isFinal)
                }
                RecordingService.BROADCAST_STATE -> {
                    val state = intent.getStringExtra(RecordingService.EXTRA_STATE) ?: return
                    val duration = intent.getLongExtra(RecordingService.EXTRA_DURATION, 0L)
                    viewModel.updateDuration(duration)
                    when (state) {
                        "PAUSED" -> viewModel.setPaused()
                        "RECORDING" -> viewModel.setResumed()
                        "STOPPED" -> viewModel.stopSession()
                    }
                }
            }
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.RECORD_AUDIO] == true) {
                showStartRecordingDialog()
            } else {
                Toast.makeText(this, "Microphone permission is required to record meetings", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferencesManager(this)

        setSupportActionBar(binding.toolbar)
        setupBottomNav()
        setupRecordPanel()
        setupHistoryPanel()
        setupTrackerPanel()
        observeViewModel()
        registerReceivers()

        if (!prefs.hasApiKey()) {
            binding.cardApiWarning.visibility = View.VISIBLE
        }

        binding.btnGoSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    // ===================== NAVIGATION =====================

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_record -> { showPanel(Panel.RECORD); true }
                R.id.nav_history -> { showPanel(Panel.HISTORY); true }
                R.id.nav_tracker -> { showPanel(Panel.TRACKER); true }
                else -> false
            }
        }
        binding.bottomNav.selectedItemId = R.id.nav_record
    }

    private enum class Panel { RECORD, HISTORY, TRACKER }
    private var currentPanel = Panel.RECORD

    private fun showPanel(panel: Panel) {
        if (panel == currentPanel) return
        currentPanel = panel

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        val panels = listOf(binding.panelRecord, binding.panelHistory, binding.panelTracker)
        val target = when (panel) {
            Panel.RECORD -> binding.panelRecord
            Panel.HISTORY -> { viewModel.loadSavedMoms(); binding.panelHistory }
            Panel.TRACKER -> { viewModel.loadTrackerItems(); binding.panelTracker }
        }

        panels.forEach { p ->
            if (p == target) {
                p.startAnimation(fadeIn)
                p.visibility = View.VISIBLE
            } else if (p.visibility == View.VISIBLE) {
                p.startAnimation(fadeOut)
                p.visibility = View.GONE
            }
        }
    }

    // ===================== RECORD PANEL =====================

    private fun setupRecordPanel() {
        transcriptAdapter = TranscriptAdapter()
        binding.rvTranscript.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            adapter = transcriptAdapter
        }

        binding.btnRecord.setOnClickListener {
            when (viewModel.recordingState.value) {
                MainViewModel.RecordingState.IDLE,
                MainViewModel.RecordingState.STOPPED -> checkAndStartRecording()
                MainViewModel.RecordingState.RECORDING -> pauseRecording()
                MainViewModel.RecordingState.PAUSED -> resumeRecording()
                else -> {}
            }
        }

        binding.btnStop.setOnClickListener { confirmStopRecording() }

        binding.btnGenerateMom.setOnClickListener {
            if (prefs.hasApiKey()) showGenerateMomDialog()
            else showApiKeyRequiredDialog()
        }

        binding.btnClearTranscript.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Transcript")
                .setMessage("Clear the current transcript?")
                .setPositiveButton("Clear") { _, _ -> viewModel.startSession() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ===================== HISTORY PANEL =====================

    private fun setupHistoryPanel() {
        momListAdapter = MomListAdapter(
            onClick = { mom -> openMomDetail(mom) },
            onDelete = { mom ->
                AlertDialog.Builder(this)
                    .setTitle("Delete MOM")
                    .setMessage("Delete '${mom.meetingTitle.ifBlank { "this meeting" }}'?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteMom(mom.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvMomHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = momListAdapter
        }
    }

    // ===================== TRACKER PANEL =====================

    private fun setupTrackerPanel() {
        trackerAdapter = TrackerAdapter { meetingId, actionId, newStatus ->
            viewModel.updateActionStatus(meetingId, actionId, newStatus)
        }
        binding.rvTracker.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = trackerAdapter
        }

        binding.chipAll.setOnClickListener { viewModel.setTrackerFilter("ALL") }
        binding.chipPending.setOnClickListener { viewModel.setTrackerFilter("PENDING") }
        binding.chipInProgress.setOnClickListener { viewModel.setTrackerFilter("IN_PROGRESS") }
        binding.chipDone.setOnClickListener { viewModel.setTrackerFilter("COMPLETED") }
    }

    // ===================== OBSERVERS =====================

    private fun observeViewModel() {
        viewModel.transcriptEntries.observe(this) { entries ->
            transcriptAdapter.submitList(entries.toList())
            if (entries.isNotEmpty()) {
                binding.rvTranscript.smoothScrollToPosition(entries.size - 1)
            }
            val hasContent = entries.any { it.isFinal }
            binding.btnGenerateMom.isEnabled = hasContent
            binding.btnClearTranscript.isEnabled = entries.isNotEmpty()
        }

        viewModel.recordingState.observe(this) { state ->
            updateRecordingUI(state)
        }

        viewModel.duration.observe(this) { millis ->
            binding.tvDuration.text = formatDuration(millis)
        }

        viewModel.momGenerating.observe(this) { generating ->
            binding.progressMom.visibility = if (generating) View.VISIBLE else View.GONE
            binding.btnGenerateMom.isEnabled = !generating &&
                (viewModel.transcriptEntries.value?.any { it.isFinal } == true)
        }

        viewModel.generatedMom.observe(this) { mom ->
            mom ?: return@observe
            showMomReadyDialog(mom)
        }

        viewModel.error.observe(this) { error ->
            error ?: return@observe
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }

        viewModel.savedMoms.observe(this) { moms ->
            momListAdapter.submitList(moms)
            binding.tvEmptyHistory.visibility = if (moms.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.trackerItems.observe(this) { items ->
            trackerAdapter.submitList(items)
            binding.tvEmptyTracker.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.rvTracker.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    // ===================== RECORDING UI =====================

    private fun updateRecordingUI(state: MainViewModel.RecordingState) {
        when (state) {
            MainViewModel.RecordingState.IDLE,
            MainViewModel.RecordingState.STOPPED -> {
                binding.btnRecord.setImageResource(R.drawable.ic_mic)
                binding.btnRecord.contentDescription = "Start Recording"
                binding.btnStop.visibility = View.GONE
                binding.tvRecordingStatus.text = "Tap to start recording"
                binding.tvRecordingStatus.setTextColor(getColor(R.color.on_surface_variant))
                binding.recordingPulse.visibility = View.GONE
                binding.waveformContainer.visibility = View.GONE
                binding.tvDuration.text = "00:00"
                stopWaveAnimation()
                stopPulseAnimation()
            }
            MainViewModel.RecordingState.RECORDING -> {
                binding.btnRecord.setImageResource(R.drawable.ic_pause)
                binding.btnRecord.contentDescription = "Pause Recording"
                binding.btnStop.visibility = View.VISIBLE
                binding.tvRecordingStatus.text = "Recording..."
                binding.tvRecordingStatus.setTextColor(getColor(R.color.error))
                binding.recordingPulse.visibility = View.VISIBLE
                binding.waveformContainer.visibility = View.VISIBLE
                startWaveAnimation()
                startPulseAnimation()
            }
            MainViewModel.RecordingState.PAUSED -> {
                binding.btnRecord.setImageResource(R.drawable.ic_mic)
                binding.btnRecord.contentDescription = "Resume Recording"
                binding.btnStop.visibility = View.VISIBLE
                binding.tvRecordingStatus.text = "Paused — tap to resume"
                binding.tvRecordingStatus.setTextColor(getColor(R.color.on_surface_variant))
                binding.recordingPulse.visibility = View.GONE
                binding.waveformContainer.visibility = View.GONE
                stopWaveAnimation()
                stopPulseAnimation()
            }
        }
    }

    // ===================== WAVE ANIMATION =====================

    private fun startWaveAnimation() {
        val bars = listOf(
            binding.waveBar1, binding.waveBar2, binding.waveBar3,
            binding.waveBar4, binding.waveBar5
        )
        val durations = longArrayOf(400, 300, 500, 350, 450)
        val maxScales = floatArrayOf(3f, 2.5f, 4f, 2.8f, 3.2f)

        val animators = bars.mapIndexed { i, bar ->
            ObjectAnimator.ofFloat(bar, "scaleY", 1f, maxScales[i], 1f).apply {
                duration = durations[i]
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                startDelay = (i * 80).toLong()
            }
        }
        waveAnimatorSet = AnimatorSet().apply {
            playTogether(*animators.toTypedArray())
            start()
        }
    }

    private fun stopWaveAnimation() {
        waveAnimatorSet?.cancel()
        waveAnimatorSet = null
        listOf(binding.waveBar1, binding.waveBar2, binding.waveBar3,
            binding.waveBar4, binding.waveBar5).forEach { it.scaleY = 1f }
    }

    private fun startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofFloat(binding.recordingPulse, "alpha", 0.7f, 0f).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.recordingPulse.alpha = 1f
    }

    // ===================== RECORDING CONTROL =====================

    private fun checkAndStartRecording() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isEmpty()) showStartRecordingDialog()
        else requestPermissions.launch(permissions.toTypedArray())
    }

    private fun showStartRecordingDialog() {
        val platforms = arrayOf(
            "WhatsApp Video Call", "Instagram Video Call",
            "Zoom", "Google Meet", "Teams", "Phone Call", "Other"
        )
        var selectedPlatform = 0
        AlertDialog.Builder(this)
            .setTitle("Start Recording")
            .setMessage("Shefali will capture speech from your microphone during the call.\n\nSelect the platform:")
            .setSingleChoiceItems(platforms, 0) { _, which -> selectedPlatform = which }
            .setPositiveButton("Start") { _, _ ->
                startRecordingService(platforms[selectedPlatform])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRecordingService(platform: String) {
        viewModel.startSession(title = "Meeting on $platform")
        startForegroundService(Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        })
    }

    private fun pauseRecording() {
        startService(Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        })
    }

    private fun resumeRecording() {
        startService(Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        })
    }

    private fun confirmStopRecording() {
        AlertDialog.Builder(this)
            .setTitle("Stop Recording")
            .setMessage("Stop the current recording session?")
            .setPositiveButton("Stop") { _, _ ->
                startService(Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                })
                viewModel.stopSession()
            }
            .setNegativeButton("Keep Recording", null)
            .show()
    }

    // ===================== DIALOGS =====================

    private fun showGenerateMomDialog() {
        val titleInput = android.widget.EditText(this).apply {
            hint = "Meeting title (optional)"
            setPadding(64, 32, 64, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Generate MOM Report")
            .setMessage("AI will analyze the transcript (Hindi + English) and produce a professional English MOM.")
            .setView(titleInput)
            .setPositiveButton("Generate") { _, _ ->
                viewModel.generateMom(
                    meetingTitle = titleInput.text.toString().trim(),
                    platform = prefs.defaultMeetingPlatform
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMomReadyDialog(mom: MomReport) {
        AlertDialog.Builder(this)
            .setTitle("MOM Ready!")
            .setMessage("Report generated with ${mom.actionItems.size} action item(s).")
            .setPositiveButton("View MOM") { _, _ -> openMomDetail(mom) }
            .setNeutralButton("View Later", null)
            .show()
    }

    private fun showApiKeyRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("API Key Required")
            .setMessage("A Claude API key is required to generate MOM reports. Please add it in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openMomDetail(mom: MomReport) {
        startActivity(Intent(this, MomDetailActivity::class.java).apply {
            putExtra(MomDetailActivity.EXTRA_MOM, mom)
        })
    }

    // ===================== UTILS =====================

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(RecordingService.BROADCAST_TRANSCRIPT)
            addAction(RecordingService.BROADCAST_STATE)
        }
        ContextCompat.registerReceiver(
            this, transcriptReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun formatDuration(millis: Long): String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(millis)
        val secs = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", mins, secs)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWaveAnimation()
        stopPulseAnimation()
        try { unregisterReceiver(transcriptReceiver) } catch (e: Exception) { /* ignore */ }
    }
}
