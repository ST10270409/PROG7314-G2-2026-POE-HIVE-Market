package com.hivemarket.app.di

import com.hivemarket.app.data.local.AppCompatLocaleSwitcher
import com.hivemarket.app.data.local.LocaleSwitcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindLocaleSwitcher(impl: AppCompatLocaleSwitcher): LocaleSwitcher
}
