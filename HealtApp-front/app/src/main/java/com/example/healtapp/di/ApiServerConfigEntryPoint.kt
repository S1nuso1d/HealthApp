package com.example.healtapp.di

import com.example.healtapp.data.network.ApiServerConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApiServerConfigEntryPoint {
    fun apiServerConfig(): ApiServerConfig
}
