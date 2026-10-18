package com.tejas.rideautoaccept

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoAcceptService : AccessibilityService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var cfg = Config(false, "", emptyList(), "", 0.0, 0.0)
    private var lastClickAt = 0L

    private val fareRegex = Regex("""₹\s*([0-9]+(?:\.[0-9]+)?)""")
    private val distanceRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(km|m)\b""", RegexOption.IGNORE_CASE)

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppSettings.prefs(this).registerOnSharedPreferenceChangeListener(this)
        reload()
        AppSettings.setStatus(this, "Service connected")
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        reload()
    }

    private fun reload() {
        cfg = AppSettings.load(this)
        // Only listen to the chosen app. With no package set, listen to nothing.
        val info = serviceInfo ?: return
        info.packageNames = if (cfg.targetPackage.isNotEmpty())
            arrayOf(cfg.targetPackage)
        else
            arrayOf("com.tejas.rideautoaccept.none")
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !cfg.enabled || cfg.targetPackage.isEmpty()) return
        if (event.packageName?.toString() != cfg.targetPackage) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAt < 1500) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != cfg.targetPackage) return

        val button = findAcceptButton(root) ?: return

        if (!passesFilters(root)) {
            lastClickAt = now
            AppSettings.setStatus(this, "Offer skipped (did not match your rules)")
            return
        }

        if (clickNode(button)) {
            lastClickAt = now
            AppSettings.setStatus(this, "Tapped Accept")
        }
    }

    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (cfg.acceptViewId.isNotEmpty()) {
            root.findAccessibilityNodeInfosByViewId(cfg.acceptViewId)
                ?.firstOrNull()?.let { return it }
        }
        for (label in cfg.acceptLabels) {
            root.findAccessibilityNodeInfosByText(label)
                ?.firstOrNull { matchesLabel(it) }?.let { return it }
        }
        return null
    }

    private fun matchesLabel(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        return cfg.acceptLabels.any {
            it.equals(text, ignoreCase = true) || it.equals(desc, ignoreCase = true)
        }
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        var hops = 0
        while (n != null && !n.isClickable && hops < 6) {
            n = n.parent
            hops++
        }
        return n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun passesFilters(root: AccessibilityNodeInfo): Boolean {
        if (cfg.minFare <= 0.0 && cfg.maxPickupKm <= 0.0) return true

        val sb = StringBuilder()
        collectText(root, sb, 0)
        val text = sb.toString()

        if (cfg.minFare > 0.0) {
            val fare = fareRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            if (fare != null && fare < cfg.minFare) return false
        }

        if (cfg.maxPickupKm > 0.0) {
            val match = distanceRegex.find(text)
            if (match != null) {
                val value = match.groupValues[1].toDoubleOrNull()
                val unit = match.groupValues[2].lowercase()
                if (value != null) {
                    val km = if (unit == "m") value / 1000.0 else value
                    if (km > cfg.maxPickupKm) return false
                }
            }
        }
        return true
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null || depth > 25) return
        node.text?.let { sb.append(it).append('\n') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb, depth + 1)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        AppSettings.prefs(this).unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }
}
