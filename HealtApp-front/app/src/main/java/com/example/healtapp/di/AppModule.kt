package com.example.healtapp.di

import android.content.Context
import com.example.healtapp.core.common.Constants
import com.example.healtapp.data.network.api.ActivityApi
import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.api.AuthApi
import com.example.healtapp.data.network.api.HydrationApi
import com.example.healtapp.data.network.api.MealApi
import com.example.healtapp.data.network.api.ProfileApi
import com.example.healtapp.data.network.api.SleepApi
import com.example.healtapp.data.network.interceptor.AuthInterceptor
import com.example.healtapp.data.preferences.TokenStorage
import com.example.healtapp.data.repository.ActivityRepositoryImpl
import com.example.healtapp.data.repository.AiRepositoryImpl
import com.example.healtapp.data.repository.AuthRepositoryImpl
import com.example.healtapp.data.repository.HydrationRepositoryImpl
import com.example.healtapp.data.repository.MealRepositoryImpl
import com.example.healtapp.data.repository.ProfileRepositoryImpl
import com.example.healtapp.data.repository.SleepRepositoryImpl
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.AiRepository
import com.example.healtapp.domain.repository.AuthRepository
import com.example.healtapp.domain.repository.HydrationRepository
import com.example.healtapp.domain.repository.MealRepository
import com.example.healtapp.domain.repository.ProfileRepository
import com.example.healtapp.domain.repository.SleepRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppModule {

    @Volatile
    private var retrofit: Retrofit? = null

    private fun provideTokenStorage(context: Context): TokenStorage {
        return TokenStorage(context)
    }

    private fun provideOkHttp(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(provideTokenStorage(context)))
            .build()
    }

    private fun provideRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(provideOkHttp(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .also { retrofit = it }
        }
    }

    fun provideAuthApi(context: Context): AuthApi {
        return provideRetrofit(context).create(AuthApi::class.java)
    }

    fun provideProfileApi(context: Context): ProfileApi {
        return provideRetrofit(context).create(ProfileApi::class.java)
    }

    fun provideSleepApi(context: Context): SleepApi {
        return provideRetrofit(context).create(SleepApi::class.java)
    }

    fun provideHydrationApi(context: Context): HydrationApi {
        return provideRetrofit(context).create(HydrationApi::class.java)
    }

    fun provideActivityApi(context: Context): ActivityApi {
        return provideRetrofit(context).create(ActivityApi::class.java)
    }

    fun provideMealApi(context: Context): MealApi {
        return provideRetrofit(context).create(MealApi::class.java)
    }

    fun provideAiApi(context: Context): AiApi {
        return provideRetrofit(context).create(AiApi::class.java)
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return AuthRepositoryImpl(
            authApi = provideAuthApi(context),
            tokenStorage = provideTokenStorage(context)
        )
    }

    fun provideProfileRepository(context: Context): ProfileRepository {
        return ProfileRepositoryImpl(
            provideProfileApi(context)
        )
    }

    fun provideSleepRepository(context: Context): SleepRepository {
        return SleepRepositoryImpl(
            provideSleepApi(context)
        )
    }

    fun provideHydrationRepository(context: Context): HydrationRepository {
        return HydrationRepositoryImpl(
            provideHydrationApi(context)
        )
    }

    fun provideActivityRepository(context: Context): ActivityRepository {
        return ActivityRepositoryImpl(
            api = provideActivityApi(context)
        )
    }

    fun provideMealRepository(context: Context): MealRepository {
        return MealRepositoryImpl(
            api = provideMealApi(context)
        )
    }

    fun provideAiRepository(context: Context): AiRepository {
        return AiRepositoryImpl(
            aiApi = provideAiApi(context)
        )
    }
}