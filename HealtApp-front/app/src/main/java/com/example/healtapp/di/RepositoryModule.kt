package com.example.healtapp.di

import com.example.healtapp.data.repository.ActivityRepositoryImpl
import com.example.healtapp.data.repository.AiRepositoryImpl
import com.example.healtapp.data.repository.AuthRepositoryImpl
import com.example.healtapp.data.repository.HydrationRepositoryImpl
import com.example.healtapp.data.repository.ImportRepositoryImpl
import com.example.healtapp.data.repository.MealRepositoryImpl
import com.example.healtapp.data.repository.ProfileRepositoryImpl
import com.example.healtapp.data.repository.SleepRepositoryImpl
import com.example.healtapp.data.repository.WellnessRepositoryImpl
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.AiRepository
import com.example.healtapp.domain.repository.AuthRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.ImportRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import com.example.healtapp.domain.repository.WellnessRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository

    @Binds
    @Singleton
    abstract fun bindHydrationRepository(impl: HydrationRepositoryImpl): HydrationRepository

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindImportRepository(impl: ImportRepositoryImpl): ImportRepository

    @Binds
    @Singleton
    abstract fun bindWellnessRepository(impl: WellnessRepositoryImpl): WellnessRepository
}

