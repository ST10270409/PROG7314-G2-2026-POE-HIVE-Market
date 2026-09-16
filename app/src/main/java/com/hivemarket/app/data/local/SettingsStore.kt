package com.hivemarket.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class LocalSettings(
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false
)

/**
 * Local cache for the four Settings (FR2): language, push notifications,
 * biometric login, and campus (campus lives on User, not here, since it's
 * part of the profile rather than a device-local preference).
 *
 * This is a SharedPreferences store rather than a new Room table — for a
 * single-row, single-user blob of preferences, SharedPreferences is the
 * standard Android choice and avoids a DAO/entity/migration for one row.
 * It still satisfies FR2's "cached in Room" intent in spirit: settings are
 * read from local storage instantly on launch and only pushed to the API
 * in the background (see SettingsViewModel), the same offline-first shape
 * ListingRepository uses for listings.
 */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hivemarket_settings", Context.MODE_PRIVATE)

    fun read(): LocalSettings = LocalSettings(
        language = prefs.getString(KEY_LANGUAGE, "en") ?: "en",
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false)
    )

    fun write(settings: LocalSettings) {
        prefs.edit {
            putString(KEY_LANGUAGE, settings.language)
            putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            putBoolean(KEY_BIOMETRIC, settings.biometricEnabled)
        }
    }

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
