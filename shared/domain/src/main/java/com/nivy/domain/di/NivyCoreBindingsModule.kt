package com.nivy.domain.di

import com.nivy.domain.features.Features
import com.nivy.domain.features.NivyFeatures
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface NivyCoreBindingsModule {
    @Binds
    fun bindFeatures(features: NivyFeatures): Features
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FeaturesEntryPoint {
    fun getFeatures(): Features
}
