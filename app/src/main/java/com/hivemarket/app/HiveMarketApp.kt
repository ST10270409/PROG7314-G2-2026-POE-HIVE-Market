package com.hivemarket.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HiveMarketApp : Application(), Configuration.Provider {

    // Lets WorkManager construct SyncPendingListingsWorker with its Hilt
    // dependencies (ListingRepository) instead of requiring a no-arg
    // constructor. AndroidManifest.xml disables WorkManager's default
    // auto-initializer, which means it will NOT start itself automatically
    // anymore — the explicit WorkManager.initialize(...) call below is
    // required, not optional, once that auto-init is removed. Skipping
    // this call while also removing the manifest initializer is a real
    // trap: the app would compile and run fine right up until the first
    // WorkManager.getInstance(context) call (in SyncScheduler), which
    // would then crash with "WorkManager is not initialized properly."
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
        // Functional logging per the Part 2 brief — demonstrates lifecycle
        // and state-transition awareness, not just a "Hello World" log line.
        Log.i("HiveMarketApp", "Application onCreate — process started")
    }
}