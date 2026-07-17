package com.example.uniconnect_iq

import com.google.gson.annotations.SerializedName

// ==================== USUARIOS ====================

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

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("password") val password: String
)

// ==================== VOIP / IA ====================

data class LlamadaRequest(
    @SerializedName("nombreDestino") val nombreDestino: String,
    @SerializedName("extensionOrigen") val extensionOrigen: String
)

// ==================== RADAR ====================
data class VinculacionRequest(

    @SerializedName("extension")
    val extension:String,

    @SerializedName("nombre")
    val nombre:String,

    @SerializedName("dispositivo")
    val dispositivo:String? = null,


    @SerializedName("token")
    val token:String? = null

)