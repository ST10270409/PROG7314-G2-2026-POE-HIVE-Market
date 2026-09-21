package com.hivemarket.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val listingId: String,
    val title: String,
    val price: Double,
    val imageUrl: String?,
    val isAvailable: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface FavouriteDao {

    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllFavourites(): Flow<List<FavouriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE listingId = :listingId)")
    fun isFavourite(listingId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE listingId = :listingId)")
    suspend fun isFavouriteSync(listingId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE listingId = :listingId")
    suspend fun deleteFavouriteById(listingId: String)

    @Query("DELETE FROM favourites")
    suspend fun clearAll()
}