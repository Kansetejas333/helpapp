package com.tejas.rideautoaccept

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Config(
    val enabled: Boolean,
    val targetPackage: String,
    val acceptLabels: List<String>,
    val acceptViewId: String,
    val minFare: Double,
    val maxPickupKm: Double
)

object AppSettings {
    private const val FILE = "ride_auto_accept"
    private const val STATUS_FILE = "ride_auto_accept_status"

    const val KEY_ENABLED = "enabled"
    const val KEY_PACKAGE = "package"
    const val KEY_LABELS = "labels"
    const val KEY_VIEW_ID = "view_id"
    const val KEY_MIN_FARE = "min_fare"
    const val KEY_MAX_KM = "max_km"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context): Config {
        val p = prefs(ctx)
        return Config(
            enabled = p.getBoolean(KEY_ENABLED, false),
            targetPackage = (p.getString(KEY_PACKAGE, "") ?: "").trim(),
            acceptLabels = (p.getString(KEY_LABELS, "Accept") ?: "Accept")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() },
            acceptViewId = (p.getString(KEY_VIEW_ID, "") ?: "").trim(),
            minFare = (p.getString(KEY_MIN_FARE, "") ?: "").trim().toDoubleOrNull() ?: 0.0,
            maxPickupKm = (p.getString(KEY_MAX_KM, "") ?: "").trim().toDoubleOrNull() ?: 0.0
        )
    }

    // Status lives in a separate file so updating it never triggers a settings reload.
    fun setStatus(ctx: Context, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        ctx.getSharedPreferences(STATUS_FILE, Context.MODE_PRIVATE)
            .edit().putString("last", "$time  $message").apply()
    }

    fun getStatus(ctx: Context): String =
        ctx.getSharedPreferences(STATUS_FILE, Context.MODE_PRIVATE)
            .getString("last", "No activity yet") ?: "No activity yet"
}
