package com.example.healtapp.di

import com.example.healtapp.data.repository.CycleRepositoryImpl
import com.example.healtapp.domain.repository.CycleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CycleRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCycleRepository(
        impl: CycleRepositoryImpl
    ): CycleRepository
}
