package com.hivemarket.app.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hivemarket.app.data.repository.ListingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real auto-sync for Offline Drafts (FR4/FR9) — this is what upgrades
 * the feature from "manual retry button" to "syncs automatically once
 * you're back online," which the README previously flagged as a known
 * gap for one of the group's 3 chosen features.
 *
 * Runs whenever WorkManager decides its NetworkType.CONNECTED constraint
 * is satisfied — which can be immediately (if already online) or after a
 * real delay (if the phone was offline when the listing was created and
 * only reconnects later, even after the app process has been killed —
 * WorkManager persists enqueued work across process death and reboot on
 * its own, so nothing extra is needed for that case).
 */
@HiltWorker
class SyncPendingListingsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ListingRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val failures = repository.syncAllPendingDrafts()
        return if (failures == 0) {
            Result.success()
        } else {
            // WorkManager retries with exponential backoff while the
            // NetworkType.CONNECTED constraint still holds. A persistent
            // non-network failure (e.g. a validation error the server
            // rejects every time) would retry indefinitely under this —
            // an acceptable tradeoff for a Part 2 prototype, worth
            // revisiting with a max-attempt count before any real
            // production use.
            Result.retry()
        }
    }
}

/**
 * Thin wrapper so ViewModels call scheduleSync() rather than touching
 * WorkManager's builder APIs directly — keeps WorkManager specifics in
 * one place, and this file is the one to look at if the sync behaviour
 * ever needs to change (e.g. adding a max retry count).
 */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        // A single, well-known name so multiple offline-created listings
        // all get picked up by one Worker run (syncAllPendingDrafts loops
        // over every pending draft) rather than queuing a separate Worker
        // per listing.
        private const val WORK_NAME = "sync_pending_listings"
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncPendingListingsWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf()) // no input needed — the Worker reads pending drafts itself
            .build()

        // APPEND_OR_REPLACE: if a sync attempt is already pending/running,
        // this new request chains after it rather than being dropped or
        // spawning a duplicate — matters when the user creates several
        // offline listings in a row.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}