package com.example.foodieclub.data.network // Revisa tu paquete

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor // Para logs
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // --- IMPORTANTE: URL BASE DE TU API ---
    // Si pruebas en Emulador Android, usa 10.0.2.2 para referirte al localhost de tu PC
    private const val BASE_URL_EMULATOR = "http://10.0.2.2:8080/api/"
    // Si pruebas en un dispositivo físico conectado a la MISMA red Wi-Fi que tu PC:
    // 1. Averigua la IP local de tu PC (ej. en Windows: ipconfig, en macOS/Linux: ifconfig o ip addr)
    // 2. Reemplaza "TU_IP_LOCAL" por esa IP. Ejemplo: "http://192.168.1.100:8080/api/"
    private const val BASE_URL_DEVICE = "http://172.20.10.12:8080/api/"

    // Selecciona la URL adecuada (cambia esto según dónde pruebes)
    private const val ACTIVE_BASE_URL = BASE_URL_EMULATOR // O BASE_URL_DEVICE

    // --- Configuración del cliente OkHttp (para logs, timeouts, interceptors) ---
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Loguea TODO: cabeceras y cuerpo de petición/respuesta
        // Para producción, cambia a Level.BASIC o Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Añade el interceptor de logs
        .connectTimeout(30, TimeUnit.SECONDS) // Timeout de conexión
        .readTimeout(30, TimeUnit.SECONDS)    // Timeout de lectura
        .writeTimeout(30, TimeUnit.SECONDS)   // Timeout de escritura
        // --- Aquí añadiremos el interceptor de autenticación MÁS ADELANTE ---
        .build()

    // --- Instancia de Retrofit ---
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(ACTIVE_BASE_URL)
            .client(okHttpClient) // Usa el cliente OkHttp configurado
            .addConverterFactory(GsonConverterFactory.create()) // Usa Gson para convertir JSON
            .build()

        retrofit.create(ApiService::class.java) // Crea la implementación de nuestra interfaz
    }
}