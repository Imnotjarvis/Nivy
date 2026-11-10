package com.nivy.base.di

import com.nivy.base.threading.DispatchersProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {
    // One scope for the app's lifetime: unscoped, each injection made its own.
    @AppCoroutineScope
    @Provides
    @Singleton
    fun provideApplicationCoroutineScope(
        dispatchers: DispatchersProvider
    ): CoroutineScope {
        val applicationJob = SupervisorJob()
        return CoroutineScope(dispatchers.main + applicationJob)
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class AppCoroutineScope
