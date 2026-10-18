package com.tejas.rideautoaccept

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoAcceptService : AccessibilityService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var cfg = Config(false, emptyList())
    private val lastActionAt = HashMap<String, Long>()

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
        // Only listen to the apps you configured. With none set, listen to nothing.
        val info = serviceInfo ?: return
        val pkgs = cfg.profiles.map { it.packageName }
        info.packageNames = if (pkgs.isNotEmpty())
            pkgs.toTypedArray()
        else
            arrayOf("com.tejas.rideautoaccept.none")
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !cfg.enabled) return
        val pkg = event.packageName?.toString() ?: return
        val profile = cfg.profiles.firstOrNull { it.packageName == pkg } ?: return

        val now = SystemClock.elapsedRealtime()
        if (now - (lastActionAt[pkg] ?: 0L) < 1500) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val button = findAcceptButton(root, profile) ?: return

        if (!passesFilters(root, profile)) {
            lastActionAt[pkg] = now
            AppSettings.setStatus(this, "[$pkg] Offer skipped (did not match your rules)")
            return
        }

        if (clickNode(button)) {
            lastActionAt[pkg] = now
            AppSettings.setStatus(this, "[$pkg] Tapped Accept")
        }
    }

    private fun findAcceptButton(root: AccessibilityNodeInfo, profile: AppProfile): AccessibilityNodeInfo? {
        if (profile.acceptViewId.isNotEmpty()) {
            root.findAccessibilityNodeInfosByViewId(profile.acceptViewId)
                ?.firstOrNull()?.let { return it }
        }
        for (label in profile.acceptLabels) {
            root.findAccessibilityNodeInfosByText(label)
                ?.firstOrNull { matchesLabel(it, profile) }?.let { return it }
        }
        return null
    }

    private fun matchesLabel(node: AccessibilityNodeInfo, profile: AppProfile): Boolean {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        return profile.acceptLabels.any {
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

    private fun passesFilters(root: AccessibilityNodeInfo, profile: AppProfile): Boolean {
        if (profile.minFare <= 0.0 && profile.maxPickupKm <= 0.0) return true

        val sb = StringBuilder()
        collectText(root, sb, 0)
        val text = sb.toString()

        if (profile.minFare > 0.0) {
            val fare = fareRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            if (fare != null && fare < profile.minFare) return false
        }

        if (profile.maxPickupKm > 0.0) {
            val match = distanceRegex.find(text)
            if (match != null) {
                val value = match.groupValues[1].toDoubleOrNull()
                val unit = match.groupValues[2].lowercase()
                if (value != null) {
                    val km = if (unit == "m") value / 1000.0 else value
                    if (km > profile.maxPickupKm) return false
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
