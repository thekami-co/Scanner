package com.scann.thekami.util

import android.content.Context
import android.os.Build

data class AppSettings(
    val floatingNav: Boolean = false,
    val dynamicColor: Boolean = true,
    val animations: Boolean = true
)

object AppPreferences {
    private const val PREFS_NAME = "scann_prefs"
    private const val KEY_FLOATING_NAV = "floating_nav"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_ANIMATIONS = "animations"

    private const val KEY_FIRST_OPEN = "rating_first_open"
    private const val KEY_SCAN_COUNT = "rating_scan_count"
    private const val KEY_PROMPT_COUNT = "rating_prompt_count"
    private const val KEY_LAST_PROMPT = "rating_last_prompt"
    private const val KEY_PROMPT_DONE = "rating_prompt_done"

    private const val MIN_SCANS = 5
    private const val MIN_DAYS_SINCE_FIRST_OPEN = 3
    private const val DAYS_BETWEEN_PROMPTS = 7
    private const val MAX_PROMPTS = 3
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    val dynamicColorSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): AppSettings {
        val prefs = prefs(context)
        val defaults = AppSettings()
        return AppSettings(
            floatingNav = prefs.getBoolean(KEY_FLOATING_NAV, defaults.floatingNav),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            animations = prefs.getBoolean(KEY_ANIMATIONS, defaults.animations)
        )
    }

    fun save(context: Context, settings: AppSettings) {
        prefs(context)
            .edit()
            .putBoolean(KEY_FLOATING_NAV, settings.floatingNav)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putBoolean(KEY_ANIMATIONS, settings.animations)
            .apply()
    }

    // ---- Rating prompt tracking ----

    /** Call once on app start. Remembers when the app was first opened with this feature. */
    fun recordAppOpen(context: Context) {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_FIRST_OPEN)) {
            prefs.edit().putLong(KEY_FIRST_OPEN, System.currentTimeMillis()).apply()
        }
    }

    fun recordSuccessfulScan(context: Context) {
        val prefs = prefs(context)
        prefs.edit().putInt(KEY_SCAN_COUNT, prefs.getInt(KEY_SCAN_COUNT, 0) + 1).apply()
    }

    fun shouldShowRatingPrompt(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_PROMPT_DONE, false)) return false
        if (prefs.getInt(KEY_PROMPT_COUNT, 0) >= MAX_PROMPTS) return false
        if (prefs.getInt(KEY_SCAN_COUNT, 0) < MIN_SCANS) return false

        val firstOpen = prefs.getLong(KEY_FIRST_OPEN, 0L)
        if (firstOpen == 0L || now - firstOpen < MIN_DAYS_SINCE_FIRST_OPEN * DAY_MILLIS) return false

        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        if (lastPrompt != 0L && now - lastPrompt < DAYS_BETWEEN_PROMPTS * DAY_MILLIS) return false

        return true
    }

    /** User saw the prompt and chose "Not now" (or dismissed it). It will come back later, up to a limit. */
    fun onRatingPromptPostponed(context: Context) {
        val prefs = prefs(context)
        prefs.edit()
            .putInt(KEY_PROMPT_COUNT, prefs.getInt(KEY_PROMPT_COUNT, 0) + 1)
            .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
            .apply()
    }

    /** User rated or chose "Don't ask again". Never show the prompt again. */
    fun onRatingPromptFinished(context: Context) {
        prefs(context).edit().putBoolean(KEY_PROMPT_DONE, true).apply()
    }
}
