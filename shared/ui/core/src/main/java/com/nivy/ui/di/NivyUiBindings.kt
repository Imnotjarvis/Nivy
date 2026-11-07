package com.nivy.ui.di

import com.nivy.ui.time.DevicePreferences
import com.nivy.ui.time.TimeFormatter
import com.nivy.ui.time.impl.AndroidDateTimePicker
import com.nivy.ui.time.impl.AndroidDevicePreferences
import com.nivy.ui.time.impl.DateTimePicker
import com.nivy.ui.time.impl.NivyTimeFormatter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface NivyUiBindings {
    @Binds
    fun timeFormatter(impl: NivyTimeFormatter): TimeFormatter

    @Binds
    fun deviceTimePreferences(impl: AndroidDevicePreferences): DevicePreferences

    @Binds
    fun dateTimePicker(impl: AndroidDateTimePicker): DateTimePicker
}
