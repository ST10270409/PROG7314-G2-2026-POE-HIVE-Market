package com.hivemarket.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FR5 (real-time notification): receives pushes the API triggers when a new
 * message or offer is created on a listing/conversation the user is part of.
 *
 * This is a prototype stub — it logs the payload so you can demonstrate the
 * "state transition" logging the Part 2 brief asks for, and shows where a
 * real NotificationCompat builder would go once the API is actually sending
 * pushes.
 */
class HiveMarketMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HiveMarketFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: send this token to the API (e.g. PATCH /api/users/me) so the
        // backend knows where to deliver this device's pushes.
        Log.i(TAG, "FCM token refreshed: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from ${message.from}: ${message.data}")
        message.notification?.let {
            Log.d(TAG, "Notification body: ${it.title} — ${it.body}")
            // TODO: render as a system notification when backgrounded, or an
            // in-app banner when foregrounded, per the Planning and Design doc.
        }
    }
}
