package com.tejas.rideautoaccept

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppProfile(
    val packageName: String,
    val acceptLabels: List<String>,
    val acceptViewId: String,
    val minFare: Double,
    val maxPickupKm: Double
)

data class Config(
    val enabled: Boolean,
    val profiles: List<AppProfile>
)

object AppSettings {
    const val PROFILE_COUNT = 2

    private const val FILE = "ride_auto_accept_v2"
    private const val STATUS_FILE = "ride_auto_accept_status"

    const val KEY_ENABLED = "enabled"

    fun key(profile: Int, name: String) = "p${profile}_$name"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context): Config {
        val p = prefs(ctx)
        val profiles = (1..PROFILE_COUNT).map { i ->
            AppProfile(
                packageName = (p.getString(key(i, "package"), "") ?: "").trim(),
                acceptLabels = (p.getString(key(i, "labels"), "Accept") ?: "Accept")
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() },
                acceptViewId = (p.getString(key(i, "view_id"), "") ?: "").trim(),
                minFare = (p.getString(key(i, "min_fare"), "") ?: "").trim().toDoubleOrNull() ?: 0.0,
                maxPickupKm = (p.getString(key(i, "max_km"), "") ?: "").trim().toDoubleOrNull() ?: 0.0
            )
        }.filter { it.packageName.isNotEmpty() }
        return Config(p.getBoolean(KEY_ENABLED, false), profiles)
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
