
package com.hivemarket.app.di

import android.content.Context
import androidx.room.Room
import com.hivemarket.app.data.local.FavouriteDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HiveMarketDatabase {
        return Room.databaseBuilder(
            context,
            HiveMarketDatabase::class.java,
            "hivemarket.db"
        ).build()
    }

    @Provides
    fun provideListingDao(
        database: HiveMarketDatabase
    ): ListingDao = database.listingDao()

    @Provides
    @Singleton
    fun provideFavouriteDao(
        database: HiveMarketDatabase
    ): FavouriteDao = database.favouriteDao()
}
