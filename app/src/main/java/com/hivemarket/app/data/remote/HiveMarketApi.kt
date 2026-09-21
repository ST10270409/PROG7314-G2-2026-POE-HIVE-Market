@file:OptIn(
    kotlinx.serialization.ExperimentalSerializationApi::class,
    kotlinx.serialization.InternalSerializationApi::class
)

package com.hivemarket.app.data.remote

import com.hivemarket.app.domain.Conversation
import com.hivemarket.app.domain.FavouriteRequest
import com.hivemarket.app.domain.FavouriteResponse
import com.hivemarket.app.domain.Listing
import com.hivemarket.app.domain.Message
import com.hivemarket.app.domain.Offer
import com.hivemarket.app.domain.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Maps 1:1 to the "Endpoint specification" table in the Planning and Design
 * document (Section 5).
 *
 * Auth: the Firebase ID token is attached automatically by [AuthInterceptor]
 * (see NetworkModule), so it does not need to be passed manually per-call.
 */
interface HiveMarketApi {

    @POST("api/auth/firebase")
    suspend fun exchangeFirebaseToken(): Response<AuthResponse>

    @GET("api/users/me")
    suspend fun getMyProfile(): Response<User>

    @PATCH("api/users/me")
    suspend fun updateMyProfile(
        @Body body: UpdateProfileRequest
    ): Response<UpdatedResponse>

    @GET("api/listings")
    suspend fun getListings(
        @Query("q") query: String? = null,
        @Query("categoryID") categoryID: Int? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null
    ): Response<ListingsResponse>

    @POST("api/listings")
    suspend fun createListing(
        @Body body: CreateListingRequest
    ): Response<CreateListingResponse>

    @GET("api/listings/{listingID}")
    suspend fun getListing(
        @Path("listingID") listingID: Int
    ): Response<Listing>

    // ---------------- FAVOURITES ----------------

    @GET("api/favourites/{userId}")
    suspend fun getFavourites(
        @Path("userId") userId: String
    ): Response<List<Listing>>

    @POST("api/favourites")
    suspend fun addFavourite(
        @Body request: FavouriteRequest
    ): Response<FavouriteResponse>

    @DELETE("api/favourites/{userId}/{listingId}")
    suspend fun removeFavourite(
        @Path("userId") userId: String,
        @Path("listingId") listingId: String
    ): Response<FavouriteResponse>

    // ---------------- CONVERSATIONS ----------------

    @GET("api/conversations")
    suspend fun getConversations(): Response<ConversationsResponse>

    @POST("api/conversations")
    suspend fun startConversation(
        @Body body: StartConversationRequest
    ): Response<Conversation>

    @GET("api/conversations/{conversationID}/messages")
    suspend fun getMessages(
        @Path("conversationID") conversationID: Int
    ): Response<MessagesResponse>

    @POST("api/conversations/{conversationID}/messages")
    suspend fun sendMessage(
        @Path("conversationID") conversationID: Int,
        @Body body: SendMessageRequest
    ): Response<Message>

    @POST("api/listings/{listingID}/offers")
    suspend fun makeOffer(
        @Path("listingID") listingID: Int,
        @Body body: MakeOfferRequest
    ): Response<Offer>

    @PATCH("api/settings")
    suspend fun updateSettings(
        @Body body: SettingsRequest
    ): Response<UpdatedResponse>
}

// ---- Request/response payload shapes ----

@Serializable
data class AuthResponse(
    val userID: Int,
    val name: String,
    val email: String,
    val trustScore: Int,
    val isNewUser: Boolean
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val surname: String? = null,
    val username: String? = null
)

@Serializable
data class ListingsResponse(
    val items: List<Listing>,
    val nextPage: String? = null
)

@Serializable
data class CreateListingRequest(
    val clientId: String,
    val title: String,
    val description: String,
    val price: Double,
    val categoryID: Int,
    val condition: String? = null,
    val image: String? = null
)

@Serializable
data class CreateListingResponse(
    val listingID: Int,
    val status: String,
    val datePosted: String
)

@Serializable
data class ConversationsResponse(
    val items: List<Conversation>
)

@Serializable
data class StartConversationRequest(
    val listingID: Int
)

@Serializable
data class MessagesResponse(
    val items: List<Message>
)

@Serializable
data class SendMessageRequest(
    val content: String
)

@Serializable
data class MakeOfferRequest(
    val amount: Double,
    val message: String? = null
)

@Serializable
data class SettingsRequest(
    val language: String? = null,
    val notificationsEnabled: Boolean? = null,
    val biometricEnabled: Boolean? = null
)

@Serializable
data class UpdatedResponse(
    val updated: Boolean
)