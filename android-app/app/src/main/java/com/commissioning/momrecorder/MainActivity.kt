package com.commissioning.momrecorder

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.commissioning.momrecorder.adapter.MomListAdapter
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
    private lateinit var prefs: PreferencesManager
    private var isRecordingTabActive = true

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

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
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
        setupTabs()
        setupRecordingTab()
        setupMomListTab()
        observeViewModel()
        registerReceivers()

        // Show setup tip if no API key
        if (!prefs.hasApiKey()) {
            binding.tvApiKeyWarning.visibility = View.VISIBLE
        }
    }

    private fun setupTabs() {
        binding.tabRecording.setOnClickListener { switchTab(true) }
        binding.tabHistory.setOnClickListener { switchTab(false) }
        switchTab(true)
    }

    private fun switchTab(recording: Boolean) {
        isRecordingTabActive = recording
        binding.tabRecording.isSelected = recording
        binding.tabHistory.isSelected = !recording
        binding.layoutRecording.visibility = if (recording) View.VISIBLE else View.GONE
        binding.layoutHistory.visibility = if (!recording) View.VISIBLE else View.GONE
        binding.tabRecording.setTextColor(
            if (recording) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#99FFFFFF")
        )
        binding.tabHistory.setTextColor(
            if (!recording) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#99FFFFFF")
        )
        if (!recording) viewModel.loadSavedMoms()
    }

    private fun setupRecordingTab() {
        transcriptAdapter = TranscriptAdapter()
        binding.rvTranscript.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            adapter = transcriptAdapter
        }

        binding.btnRecord.setOnClickListener {
            when (viewModel.recordingState.value) {
                MainViewModel.RecordingState.IDLE, MainViewModel.RecordingState.STOPPED -> checkAndStartRecording()
                MainViewModel.RecordingState.RECORDING -> pauseRecording()
                MainViewModel.RecordingState.PAUSED -> resumeRecording()
                else -> {}
            }
        }

        binding.btnStop.setOnClickListener { stopRecording() }

        binding.btnGenerateMom.setOnClickListener {
            if (prefs.hasApiKey()) {
                showGenerateMomDialog()
            } else {
                showApiKeyRequiredDialog()
            }
        }

        binding.btnClearTranscript.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Transcript")
                .setMessage("Are you sure you want to clear the current transcript?")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.startSession()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupMomListTab() {
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
            binding.btnGenerateMom.isEnabled = !generating
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
    }

    private fun updateRecordingUI(state: MainViewModel.RecordingState) {
        when (state) {
            MainViewModel.RecordingState.IDLE, MainViewModel.RecordingState.STOPPED -> {
                binding.btnRecord.setImageResource(R.drawable.ic_mic)
                binding.btnRecord.contentDescription = "Start Recording"
                binding.btnStop.visibility = View.GONE
                binding.tvRecordingStatus.text = "Tap to start recording"
                binding.tvRecordingStatus.setTextColor(getColor(R.color.on_surface_variant))
                binding.recordingPulse.visibility = View.GONE
                binding.tvDuration.text = "00:00"
            }
            MainViewModel.RecordingState.RECORDING -> {
                binding.btnRecord.setImageResource(R.drawable.ic_pause)
                binding.btnRecord.contentDescription = "Pause Recording"
                binding.btnStop.visibility = View.VISIBLE
                binding.tvRecordingStatus.text = "Recording..."
                binding.tvRecordingStatus.setTextColor(getColor(R.color.error))
                binding.recordingPulse.visibility = View.VISIBLE
            }
            MainViewModel.RecordingState.PAUSED -> {
                binding.btnRecord.setImageResource(R.drawable.ic_mic)
                binding.btnRecord.contentDescription = "Resume Recording"
                binding.btnStop.visibility = View.VISIBLE
                binding.tvRecordingStatus.text = "Paused - tap to resume"
                binding.tvRecordingStatus.setTextColor(getColor(R.color.on_surface_variant))
                binding.recordingPulse.visibility = View.GONE
            }
        }
    }

    private fun checkAndStartRecording() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isEmpty()) {
            showStartRecordingDialog()
        } else {
            requestPermissions.launch(permissions.toTypedArray())
        }
    }

    private fun showStartRecordingDialog() {
        val platforms = arrayOf("WhatsApp Video Call", "Instagram Video Call", "Zoom", "Google Meet", "Teams", "Other")
        var selectedPlatform = 0

        AlertDialog.Builder(this)
            .setTitle("Start Recording")
            .setMessage("MOM Recorder will transcribe the meeting audio from your microphone.\n\nSelect the platform:")
            .setSingleChoiceItems(platforms, 0) { _, which -> selectedPlatform = which }
            .setPositiveButton("Start") { _, _ ->
                val platform = platforms[selectedPlatform]
                startRecordingService(platform)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRecordingService(platform: String) {
        viewModel.startSession(title = "Meeting on $platform")
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        startForegroundService(intent)
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

    private fun stopRecording() {
        AlertDialog.Builder(this)
            .setTitle("Stop Recording")
            .setMessage("Stop the current recording?")
            .setPositiveButton("Stop") { _, _ ->
                startService(Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                })
                viewModel.stopSession()
            }
            .setNegativeButton("Keep Recording", null)
            .show()
    }

    private fun showGenerateMomDialog() {
        val titleInput = android.widget.EditText(this).apply {
            hint = "Meeting title (optional)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Generate MOM Report")
            .setMessage("AI will analyze your transcript and generate a professional Minutes of Meeting report.")
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
            .setMessage("Your MOM report has been generated with ${mom.actionItems.size} action items.")
            .setPositiveButton("View MOM") { _, _ -> openMomDetail(mom) }
            .setNeutralButton("View Later", null)
            .show()
    }

    private fun showApiKeyRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("API Key Required")
            .setMessage("A Claude API key is required to generate MOM reports. Please add your API key in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openMomDetail(mom: MomReport) {
        val intent = Intent(this, MomDetailActivity::class.java).apply {
            putExtra(MomDetailActivity.EXTRA_MOM, mom)
        }
        startActivity(intent)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(RecordingService.BROADCAST_TRANSCRIPT)
            addAction(RecordingService.BROADCAST_STATE)
        }
        ContextCompat.registerReceiver(this, transcriptReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
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
        try { unregisterReceiver(transcriptReceiver) } catch (e: Exception) { /* ignore */ }
    }
}
