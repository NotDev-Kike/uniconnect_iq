package com.example.uniconnect_iq

import com.google.gson.annotations.SerializedName

// 1. Estructura para leer o listar usuarios de la Base de Datos
data class Usuario(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("carrera") val carrera: String,
    @SerializedName("id_rol") val idRol: Int,
    @SerializedName("extension") val extension: String,
    @SerializedName("estado") val estado: String
)

// 2. Estructura estricta para registrar un usuario en MariaDB
data class UsuarioRequest(
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

// 3. Estructura CORREGIDA para el Login (Envía tanto password_hash como password)
data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("password") val password: String
)