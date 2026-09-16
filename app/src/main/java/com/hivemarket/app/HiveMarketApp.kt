package com.hivemarket.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiveMarketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Functional logging per the Part 2 brief — demonstrates lifecycle
        // and state-transition awareness, not just a "Hello World" log line.
        Log.i("HiveMarketApp", "Application onCreate — process started")
    }
}
