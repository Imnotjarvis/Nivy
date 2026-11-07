package com.nivy.wallet.di

import com.nivy.domain.AppStarter
import com.nivy.wallet.NivyAppStarter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    abstract fun appStarter(appStarter: NivyAppStarter): AppStarter
}
