package com.hivemarket.app.di

import android.content.Context
import com.hivemarket.app.data.local.FavouriteDao
import androidx.room.Room
import com.hivemarket.app.data.local.HiveMarketDatabase
import com.hivemarket.app.data.local.ListingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HiveMarketDatabase =
        Room.databaseBuilder(context, HiveMarketDatabase::class.java, "hivemarket.db")
            .fallbackToDestructiveMigration() // fine for a prototype; write real migrations before Part 3
            .build()

    @Provides
    fun provideListingDao(db: HiveMarketDatabase): ListingDao = db.listingDao()

    @Provides
    @Singleton
    fun provideFavouriteDao(database: HiveMarketDatabase): FavouriteDao {
        return database.favouriteDao()
    }
}
