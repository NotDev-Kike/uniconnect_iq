package com.example.uniconnect_iq

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // Obtener la lista completa de usuarios
    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): Response<List<Usuario>>

    // Registrar un nuevo usuario (Formulario)
    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: UsuarioRequest): Response<Unit>

    // Iniciar Sesión (Devuelve los datos del usuario verificado si coincide la clave)
    @POST("api/usuarios/login")
    suspend fun loginUsuario(@Body login: LoginRequest): Response<Usuario>

    companion object {
        // Asegúrate de cambiar esta IP si la de tu Máquina Virtual de Node.js llega a cambiar
        private const val BASE_URL = "http://192.168.1.20:3000/"

        fun crear(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}