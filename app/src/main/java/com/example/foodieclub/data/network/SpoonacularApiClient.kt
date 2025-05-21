package com.example.foodieclub.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SpoonacularApiClient {
    private const val BASE_URL = "https://api.spoonacular.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // O .NONE para producción
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient) // Importante usar tu cliente configurado
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instancia del servicio API creada por Retrofit
    val instance: SpoonacularApiService by lazy {
        retrofit.create(SpoonacularApiService::class.java)
    }
}