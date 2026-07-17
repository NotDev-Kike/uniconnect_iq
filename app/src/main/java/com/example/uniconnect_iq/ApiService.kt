package com.example.uniconnect_iq

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): Response<List<Usuario>>

    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: UsuarioRequest): Response<Unit>

    @POST("api/usuarios/login")
    suspend fun loginUsuario(@Body login: LoginRequest): Response<Usuario>

    @POST("api/llamar")
    suspend fun iniciarLlamadaIA(@Body request: LlamadaRequest): Response<Map<String, String>>

    companion object {
        private const val BASE_URL = "http://192.168.10.3:3000/"

        fun crear(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}