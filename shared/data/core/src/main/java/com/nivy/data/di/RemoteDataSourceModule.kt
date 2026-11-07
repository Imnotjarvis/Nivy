package com.nivy.data.di

import com.nivy.data.remote.RemoteExchangeRatesDataSource
import com.nivy.data.remote.impl.RemoteExchangeRatesDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {
    @Binds
    abstract fun bindExchangeRatesDataSource(
        datasource: RemoteExchangeRatesDataSourceImpl
    ): RemoteExchangeRatesDataSource
}
