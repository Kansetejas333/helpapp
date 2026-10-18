package com.tejas.rideautoaccept

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private class Fields(root: View) {
        val title: TextView = root.findViewById(R.id.profileTitle)
        val pkg: EditText = root.findViewById(R.id.packageInput)
        val labels: EditText = root.findViewById(R.id.labelsInput)
        val viewId: EditText = root.findViewById(R.id.viewIdInput)
        val minFare: EditText = root.findViewById(R.id.minFareInput)
        val maxKm: EditText = root.findViewById(R.id.maxKmInput)
    }

    private lateinit var statusText: TextView
    private lateinit var enabledSwitch: Switch
    private val profiles = ArrayList<Fields>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        enabledSwitch = findViewById(R.id.enabledSwitch)

        val p = AppSettings.prefs(this)
        enabledSwitch.isChecked = p.getBoolean(AppSettings.KEY_ENABLED, false)

        val sectionIds = listOf(R.id.profile1, R.id.profile2)
        val titles = listOf("App 1 (for example Ola)", "App 2 (for example Uber)")
        sectionIds.forEachIndexed { idx, id ->
            val f = Fields(findViewById(id))
            val n = idx + 1
            f.title.text = titles[idx]
            f.pkg.setText(p.getString(AppSettings.key(n, "package"), ""))
            f.labels.setText(p.getString(AppSettings.key(n, "labels"), "Accept"))
            f.viewId.setText(p.getString(AppSettings.key(n, "view_id"), ""))
            f.minFare.setText(p.getString(AppSettings.key(n, "min_fare"), ""))
            f.maxKm.setText(p.getString(AppSettings.key(n, "max_km"), ""))
            profiles.add(f)
        }

        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            AppSettings.prefs(this).edit()
                .putBoolean(AppSettings.KEY_ENABLED, checked).apply()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val e = AppSettings.prefs(this).edit()
            profiles.forEachIndexed { idx, f ->
                val n = idx + 1
                e.putString(AppSettings.key(n, "package"), f.pkg.text.toString().trim())
                e.putString(AppSettings.key(n, "labels"), f.labels.text.toString().trim())
                e.putString(AppSettings.key(n, "view_id"), f.viewId.text.toString().trim())
                e.putString(AppSettings.key(n, "min_fare"), f.minFare.text.toString().trim())
                e.putString(AppSettings.key(n, "max_km"), f.maxKm.text.toString().trim())
            }
            e.apply()
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
