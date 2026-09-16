package com.hivemarket.app.di

import com.google.firebase.auth.FirebaseAuth
import com.hivemarket.app.data.remote.HiveMarketApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Points at Render (see the "Development stack and hosting" section of the
 * Planning and Design document). Swap BASE_URL for your local dev server
 * (e.g. "http://10.0.2.2:5000/") while Luke's endpoints are still being
 * stood up — the network_security_config.xml already allows that host.
 */
private const val BASE_URL = "https://hivemarket-api.onrender.com/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Attaches the caller's Firebase ID token to every request, per the
     * login sequence diagram (Figure 5): the client always sends
     * `Authorization: Bearer <token>`, and the API verifies it via the
     * Firebase Admin SDK before touching the database.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(auth: FirebaseAuth): Interceptor = Interceptor { chain ->
        val token = runBlocking {
            auth.currentUser?.getIdToken(false)?.await()?.token
        }
        val request = chain.request().newBuilder().apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // verbose for the prototype; dial back for release builds
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHiveMarketApi(retrofit: Retrofit): HiveMarketApi =
        retrofit.create(HiveMarketApi::class.java)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
