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
import com.example.healtapp.data.network.api.GamificationApi
import com.example.healtapp.data.network.api.SocialApi
import com.example.healtapp.data.network.api.StatesApi
import com.example.healtapp.data.preferences.DashboardCache
import com.example.healtapp.data.preferences.ProfileCache
import com.example.healtapp.data.preferences.PendingSyncStore
import com.example.healtapp.data.preferences.WeightHistoryStore
import com.example.healtapp.data.preferences.WidgetSnapshotStore
import com.example.healtapp.data.network.auth.DataStoreTokenProvider
import com.example.healtapp.data.network.auth.TokenProvider
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.interceptor.AuthInterceptor
import com.example.healtapp.data.network.interceptor.DynamicBaseUrlInterceptor
import com.example.healtapp.data.preferences.ApiServerPreferences
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
    fun provideApiServerPreferences(
        @ApplicationContext context: Context,
    ): ApiServerPreferences = ApiServerPreferences(context)

    @Provides
    @Singleton
    fun provideDynamicBaseUrlInterceptor(
        serverConfig: ApiServerConfig,
    ): DynamicBaseUrlInterceptor = DynamicBaseUrlInterceptor(serverConfig)

    @Provides
    @Singleton
    fun provideTokenProvider(
        impl: DataStoreTokenProvider,
    ): TokenProvider = impl

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider,
        tokenStorage: TokenStorage,
    ): AuthInterceptor = AuthInterceptor(tokenProvider, tokenStorage)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
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
            .baseUrl("http://127.0.0.1/")
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
    fun provideGamificationApi(retrofit: Retrofit): GamificationApi =
        retrofit.create(GamificationApi::class.java)

    @Provides
    @Singleton
    fun provideSocialApi(retrofit: Retrofit): SocialApi =
        retrofit.create(SocialApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardCache(
        @ApplicationContext context: Context,
    ): DashboardCache = DashboardCache(context)

    @Provides
    @Singleton
    fun provideProfileCache(
        @ApplicationContext context: Context,
    ): ProfileCache = ProfileCache(context)

    @Provides
    @Singleton
    fun provideWidgetSnapshotStore(
        @ApplicationContext context: Context,
    ): WidgetSnapshotStore = WidgetSnapshotStore(context)

    @Provides
    @Singleton
    fun provideWeightHistoryStore(
        @ApplicationContext context: Context,
    ): WeightHistoryStore = WeightHistoryStore(context)

    @Provides
    @Singleton
    fun providePendingSyncStore(
        @ApplicationContext context: Context,
    ): PendingSyncStore = PendingSyncStore(context)
}