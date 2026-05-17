package com.example.healtapp.di

import android.content.Context
import coil.ImageLoader
import com.example.healtapp.BuildConfig
import com.example.healtapp.data.network.api.ActionPlanApi
import com.example.healtapp.data.network.api.ActivityApi
import com.example.healtapp.data.network.api.AiApi
import com.example.healtapp.data.network.api.AnalyticsApi
import com.example.healtapp.data.network.api.AuthApi
import com.example.healtapp.data.network.api.DashboardApi
import com.example.healtapp.data.network.api.HealthApi
import com.example.healtapp.data.network.api.HydrationApi
import com.example.healtapp.data.network.api.ImportApi
import com.example.healtapp.data.network.api.IntegrationsApi
import com.example.healtapp.data.network.api.MealApi
import com.example.healtapp.data.network.api.ProfileApi
import com.example.healtapp.data.network.api.SleepApi
import com.example.healtapp.data.network.api.SmartApi
import com.example.healtapp.data.network.api.StatesApi
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.data.network.auth.DataStoreTokenProvider
import com.example.healtapp.data.network.auth.TokenProvider
import com.example.healtapp.data.network.interceptor.AuthInterceptor
import com.example.healtapp.data.preferences.NotificationPrefs
import com.example.healtapp.data.preferences.ThemePreferences
import com.example.healtapp.data.preferences.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context
    ): TokenStorage = TokenStorage(context)

    @Provides
    @Singleton
    fun provideNotificationPrefs(
        @ApplicationContext context: Context,
    ): NotificationPrefs = NotificationPrefs(context)

    @Provides
    @Singleton
    fun provideThemePreferences(
        @ApplicationContext context: Context,
    ): ThemePreferences = ThemePreferences(context)

    @Provides
    @Singleton
    fun provideTokenProvider(
        impl: DataStoreTokenProvider,
    ): TokenProvider = impl

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider
    ): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideSleepApi(retrofit: Retrofit): SleepApi =
        retrofit.create(SleepApi::class.java)

    @Provides
    @Singleton
    fun provideHydrationApi(retrofit: Retrofit): HydrationApi =
        retrofit.create(HydrationApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi =
        retrofit.create(HealthApi::class.java)

    @Provides
    @Singleton
    fun provideMealApi(retrofit: Retrofit): MealApi =
        retrofit.create(MealApi::class.java)

    @Provides
    @Singleton
    fun provideActivityApi(retrofit: Retrofit): ActivityApi =
        retrofit.create(ActivityApi::class.java)

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideImportApi(retrofit: Retrofit): ImportApi =
        retrofit.create(ImportApi::class.java)

    @Provides
    @Singleton
    fun provideIntegrationsApi(retrofit: Retrofit): IntegrationsApi =
        retrofit.create(IntegrationsApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi =
        retrofit.create(DashboardApi::class.java)

    @Provides
    @Singleton
    fun provideStatesApi(retrofit: Retrofit): StatesApi =
        retrofit.create(StatesApi::class.java)

    @Provides
    @Singleton
    fun provideActionPlanApi(retrofit: Retrofit): ActionPlanApi =
        retrofit.create(ActionPlanApi::class.java)

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi =
        retrofit.create(AnalyticsApi::class.java)

    @Provides
    @Singleton
    fun provideSmartApi(retrofit: Retrofit): SmartApi =
        retrofit.create(SmartApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardCache(
        @ApplicationContext context: Context,
    ): DashboardCache = DashboardCache(context)
}