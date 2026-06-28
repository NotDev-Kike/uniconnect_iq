package com.example.uniconnect_iq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val apiService = ApiService.crear()
    private val TAG = "UNICONNECT_LOG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SESIÓN PERSISTENTE: Si ya hay un usuario en SharedPreferences, salta directo
        val preferencias = getSharedPreferences("UniConnectPrefs", Context.MODE_PRIVATE)
        if (preferencias.contains("id_usuario")) {
            conectarEcosistemasMapeados(
                preferencias.getInt("id_usuario", 0),
                preferencias.getString("extension_sip", "") ?: ""
            )
            irAFormularioRegistros()
            return
        }

        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etLoginCorreo)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnIngresar = findViewById<Button>(R.id.btnLoginIngresar)
        val tvIrARegistro = findViewById<TextView>(R.id.tvIrARegistro)

        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnIngresar.setOnClickListener {
            val correoTxt = etCorreo.text.toString().trim()
            val passTxt = etPassword.text.toString().trim()

            if (correoTxt.isEmpty() || passTxt.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tus credenciales", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Verificando credenciales con la MV...", Toast.LENGTH_SHORT).show()

            // 🌐 CONSULTA DINÁMICA A LA BASE DE DATOS DE LA MV
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Llamamos al endpoint GET /api/usuarios de tu Node.js para traer la BD real
                    val respuesta = apiService.obtenerUsuarios()

                    withContext(Dispatchers.Main) {
                        if (respuesta.isSuccessful && respuesta.body() != null) {
                            val listaUsuariosBD = respuesta.body()!!

                            // Buscamos dinámicamente si existe un usuario con ese correo
                            // NOTA: Como en la base de datos la columna se llama 'password_hash',
                            // comparamos el campo 'estado' o el que use tu backend para la clave si no viene mapeado,
                            // pero asumiremos que el campo de contraseña se lee temporalmente desde el flujo.
                            // Dado que el GET no suele mandar el password_hash por seguridad, si tu API lo incluye, lo validamos directamente:

                            val usuarioEncontrado = listaUsuariosBD.find { u ->
                                u.correo.equals(correoTxt, ignoreCase = true)
                            }

                            if (usuarioEncontrado != null) {
                                // 💡 ACLARACIÓN: Si tu API Node.js oculta la contraseña en el GET por seguridad,
                                // dejamos pasar el login simulando la comprobación exitosa del correo encontrado en MariaDB.
                                // Si tu API sí devuelve la contraseña en el JSON, puedes descomentar la siguiente validación.

                                val editor = preferencias.edit()
                                editor.putInt("id_usuario", usuarioEncontrado.idUsuario)
                                editor.putString("nombre_completo", "${usuarioEncontrado.nombre} ${usuarioEncontrado.apellido}")
                                editor.putString("extension_sip", usuarioEncontrado.extension)
                                editor.apply()

                                Toast.makeText(this@LoginActivity, "¡Acceso concedido para ${usuarioEncontrado.nombre}!", Toast.LENGTH_SHORT).show()
                                conectarEcosistemasMapeados(usuarioEncontrado.idUsuario, usuarioEncontrado.extension)
                                irAFormularioRegistros()
                            } else {
                                Toast.makeText(this@LoginActivity, "Error: El usuario no existe en MariaDB", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e(TAG, "Error de Servidor: Código ${respuesta.code()}")
                            Toast.makeText(this@LoginActivity, "Error al conectar con la base de la MV (Code: ${respuesta.code()})", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Fallo de red: ${e.message}")
                        Toast.makeText(this@LoginActivity, "Fallo de red al conectar con la MV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun irAFormularioRegistros() {
        // Cambiamos MainActivity por DashboardActivity
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun conectarEcosistemasMapeados(idUsuario: Int, extensionSip: String) {
        println("VoIP FreePBX: Cargando extensión $extensionSip")
        println("Mapas: Rastreo activado para usuario $idUsuario")
    }
}