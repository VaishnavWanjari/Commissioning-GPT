package com.commissioning.momrecorder

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.commissioning.momrecorder.databinding.ActivitySettingsBinding
import com.commissioning.momrecorder.util.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Settings"
        prefs = PreferencesManager(this)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.etApiKey.setText(prefs.claudeApiKey)
        binding.etParticipants.setText(prefs.participantNames)
        binding.switchAutoGenerate.isChecked = prefs.autoGenerateMom
        val platforms = resources.getStringArray(R.array.meeting_platforms)
        val idx = platforms.indexOf(prefs.defaultMeetingPlatform)
        binding.spinnerPlatform.setSelection(if (idx >= 0) idx else 0)
    }

    private fun setupListeners() {
        binding.btnToggleApiKey.setOnClickListener {
            val isVisible = binding.etApiKey.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.etApiKey.inputType = if (isVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            binding.etApiKey.setSelection(binding.etApiKey.text.length)
        }

        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            if (apiKey.isNotBlank() && !apiKey.startsWith("sk-ant-")) {
                Toast.makeText(this, "Warning: API key should start with 'sk-ant-'", Toast.LENGTH_LONG).show()
            }
            prefs.claudeApiKey = apiKey
            prefs.participantNames = binding.etParticipants.text.toString().trim()
            prefs.autoGenerateMom = binding.switchAutoGenerate.isChecked
            val platforms = resources.getStringArray(R.array.meeting_platforms)
            prefs.defaultMeetingPlatform = platforms[binding.spinnerPlatform.selectedItemPosition]
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnGetApiKey.setOnClickListener {
            Toast.makeText(this,
                "Get your API key from: console.anthropic.com",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
