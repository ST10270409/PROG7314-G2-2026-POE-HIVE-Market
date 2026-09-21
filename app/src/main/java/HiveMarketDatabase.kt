
package com.hivemarket.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Local cache for FR4/FR9 (offline listing drafts with auto-sync).
 *
 * `clientId` is the client-generated GUID used as the idempotency key when
 * WorkManager eventually POSTs this row to /api/listings.
 * `pendingSync = true` until the API confirms the write and returns a real
 * `listingID`, at which point `serverListingId` is populated.
 */
@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val clientId: String,
    val serverListingId: Int? = null,
    val title: String,
    val description: String,
    val categoryID: Int,
    val price: Double,
    val condition: String? = null,
    val image: String? = null,
    val sellerID: Int,
    val status: String,
    val sellerName: String? = null,
    val sellerTrustScore: Int? = null,
    val pendingSync: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings ORDER BY cachedAt DESC")
    fun observeAll(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE serverListingId = :listingID LIMIT 1")
    suspend fun getById(listingID: Int): ListingEntity?

    @Query("SELECT * FROM listings WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<ListingEntity>

    @Query("SELECT * FROM listings WHERE pendingSync = 1 ORDER BY cachedAt DESC")
    fun observePendingSync(): Flow<List<ListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(listing: ListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Query("UPDATE listings SET pendingSync = 0, serverListingId = :serverListingId WHERE clientId = :clientId")
    suspend fun markSynced(clientId: String, serverListingId: Int)

    @Query("DELETE FROM listings WHERE pendingSync = 0")
    suspend fun clearSyncedCache()
}

@Database(
    entities = [
        ListingEntity::class,
        FavouriteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HiveMarketDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao

    abstract fun favouriteDao(): FavouriteDao
}
