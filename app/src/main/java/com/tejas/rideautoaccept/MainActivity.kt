package com.tejas.rideautoaccept

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var enabledSwitch: Switch
    private lateinit var packageInput: EditText
    private lateinit var labelsInput: EditText
    private lateinit var viewIdInput: EditText
    private lateinit var minFareInput: EditText
    private lateinit var maxKmInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        packageInput = findViewById(R.id.packageInput)
        labelsInput = findViewById(R.id.labelsInput)
        viewIdInput = findViewById(R.id.viewIdInput)
        minFareInput = findViewById(R.id.minFareInput)
        maxKmInput = findViewById(R.id.maxKmInput)

        val p = AppSettings.prefs(this)
        enabledSwitch.isChecked = p.getBoolean(AppSettings.KEY_ENABLED, false)
        packageInput.setText(p.getString(AppSettings.KEY_PACKAGE, ""))
        labelsInput.setText(p.getString(AppSettings.KEY_LABELS, "Accept"))
        viewIdInput.setText(p.getString(AppSettings.KEY_VIEW_ID, ""))
        minFareInput.setText(p.getString(AppSettings.KEY_MIN_FARE, ""))
        maxKmInput.setText(p.getString(AppSettings.KEY_MAX_KM, ""))

        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            AppSettings.prefs(this).edit()
                .putBoolean(AppSettings.KEY_ENABLED, checked).apply()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            AppSettings.prefs(this).edit()
                .putString(AppSettings.KEY_PACKAGE, packageInput.text.toString().trim())
                .putString(AppSettings.KEY_LABELS, labelsInput.text.toString().trim())
                .putString(AppSettings.KEY_VIEW_ID, viewIdInput.text.toString().trim())
                .putString(AppSettings.KEY_MIN_FARE, minFareInput.text.toString().trim())
                .putString(AppSettings.KEY_MAX_KM, maxKmInput.text.toString().trim())
                .apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.openAccessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val serviceState = if (isServiceEnabled()) "ON" else "OFF (turn it on in Accessibility settings)"
        statusText.text = "Accessibility service: $serviceState\nLast event: ${AppSettings.getStatus(this)}"
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val id = "$packageName/${AutoAcceptService::class.java.name}"
        return enabled.split(':').any { it.equals(id, ignoreCase = true) }
    }
}
