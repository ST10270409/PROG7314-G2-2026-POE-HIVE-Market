package com.hivemarket.app.data.local

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

/**
 * Thin wrapper around AppCompatDelegate.setApplicationLocales.
 *
 * Exists purely for testability: AppCompatDelegate calls into real Android
 * framework code that isn't available in plain JVM unit tests (calling it
 * directly from SettingsViewModel would make every test that touches
 * setLanguage() throw, since there's no Robolectric in this project to
 * provide a working Android runtime). Injecting this interface lets tests
 * substitute a no-op fake instead.
 */
interface LocaleSwitcher {
    fun apply(languageTag: String)
}

class AppCompatLocaleSwitcher @Inject constructor() : LocaleSwitcher {
    override fun apply(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
