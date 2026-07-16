package com.example.uniconnect_iq

import com.google.gson.annotations.SerializedName

// 1. Estructura para leer o listar usuarios
data class Usuario(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("carrera") val carrera: String,
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("extension") val extension: String,
    @SerializedName("estado") val estado: String
)

// 2. Estructura para registrar un usuario
data class UsuarioRequest(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("password_sip") val passwordSip: String,
    @SerializedName("carrera") val carrera: String,
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("extension") val extension: String,
    @SerializedName("estado") val estado: String
)

// 3. Estructura para el Login
data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("password") val password: String
)

// 4. Estructura para llamadas IA
data class LlamadaRequest(
    @SerializedName("nombreDestino") val nombreDestino: String,
    @SerializedName("extensionOrigen") val extensionOrigen: String
)

// 5. CORREGIDO: Estructura exacta para tu servidor Flask
data class VinculacionRequest(
    @SerializedName("mac") val mac: String, // El servidor espera 'mac'
    @SerializedName("nombreCompleto") val nombreCompleto: String // El servidor espera 'nombreCompleto'
)