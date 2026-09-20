package com.hivemarket.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain models for HiveMarket.
 *
 * Field names here deliberately match the Data Models and Schema Definitions
 * table in the Planning and Design document (Section 7) and the corrected
 * UML class diagram — e.g. `listingID`, `categoryID`, `price`, not `id`/`amount`.
 * Keeping these in sync with the API contract is what makes the
 * request/response payloads in Section 5 actually work end to end.
 */

enum class ListingStatus { Active, Sold, Reserved, Removed }
enum class OfferStatus { Pending, Accepted, Rejected, Withdrawn }
enum class MessageStatus { Sent, Delivered, Read }

@Serializable
data class User(
    val userID: Int,
    val name: String,
    val surname: String,
    val username: String,
    val email: String,
    val studentNumber: String,
    val campus: String? = null,
    val trustScore: Int = 0
)

@Serializable
data class Listing(
    val listingID: Int,
    val title: String,
    val description: String,
    val categoryID: Int,
    val price: Double,
    val condition: String? = null,
    val image: String? = null,
    val datePosted: String,
    val sellerID: Int,
    val status: String,
    // Populated by GET /api/listings/{listingID} (the detail endpoint) but
    // not by GET /api/listings (the list endpoint) — lean lists, rich detail
    // is a normal REST pattern. Left null for list-sourced Listing instances.
    val sellerName: String? = null,
    val sellerTrustScore: Int? = null,
    // Client-only field — never sent to or stored by the API. Set to true
    // when a listing is created offline and hasn't synced yet (FR4/FR9).
    val pendingSync: Boolean = false,
    // Client-only field — the local Room primary key. Two or more
    // never-synced listings all have listingID = 0 (no real server ID
    // assigned yet), so UI code that needs a unique key per item (e.g. a
    // LazyVerticalGrid) MUST key on clientId when it's present, not on
    // listingID alone — using listingID alone crashed with "Key '0' was
    // already used" the moment a second offline draft existed.
    val clientId: String? = null
)

@Serializable
data class Category(
    val categoryID: Int,
    val name: String,
    val description: String? = null
)

@Serializable
data class Offer(
    val offerID: Int,
    val listingID: Int,
    val buyerID: Int,
    val amount: Double,
    val message: String? = null,
    val dateOffered: String,
    val status: String
)

@Serializable
data class Conversation(
    val conversationID: Int,
    val listingID: Int,
    val buyerID: Int,
    val sellerID: Int,
    val lastMessage: String? = null,
    val lastUpdated: String? = null,
    @SerialName("otherParticipant") val otherParticipant: String? = null
)

@Serializable
data class Message(
    val messageID: Int,
    val conversationID: Int,
    val senderID: Int,
    val content: String,
    val status: String,
    val timestamp: String
)
