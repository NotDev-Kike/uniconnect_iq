package com.example.uniconnect_iq

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // --- Gestión de Usuarios (Puerto 3000) ---
    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): Response<List<Usuario>>

    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: UsuarioRequest): Response<Unit>

    @POST("api/usuarios/login")
    suspend fun loginUsuario(@Body login: LoginRequest): Response<Usuario>

    // --- Integración Radar (Puerto 5000) ---
    @POST("vincular_usuario")
    suspend fun vincularUsuario(@Body datos: VinculacionRequest): Response<Unit>

    // --- VoIP / IA (Puerto 3000) ---
    @POST("api/llamar")
    suspend fun iniciarLlamadaIA(@Body request: LlamadaRequest): Response<Map<String, String>>

    companion object {
        private const val BASE_URL_APP = "http://192.168.10.3:3000/"
        private const val BASE_URL_RADAR = "http://192.168.10.4:5000/"

        // Instancia principal para funciones generales (Usuarios e IA)
        fun crear(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_APP)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        // Instancia exclusiva para el Radar
        fun crearParaRadar(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_RADAR)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}