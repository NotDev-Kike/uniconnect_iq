package com.example.uniconnect_iq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private val apiService = ApiService.crear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        // 1. Verificación de sesión
        if (prefs.contains("extension")) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etLoginCorreo)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnIngresar = findViewById<Button>(R.id.btnLoginIngresar)
        val tvIrARegistro = findViewById<TextView>(R.id.tvIrARegistro)

        tvIrARegistro.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnIngresar.setOnClickListener {
            val correoIngresado = etCorreo.text.toString().trim()
            val passIngresado = etPassword.text.toString().trim()

            if (correoIngresado.isEmpty() || passIngresado.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnIngresar.isEnabled = false
            btnIngresar.text = "Validando..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuesta = apiService.obtenerUsuarios()

                    withContext(Dispatchers.Main) {
                        btnIngresar.isEnabled = true
                        btnIngresar.text = "INGRESAR"

                        if (respuesta.isSuccessful) {
                            // DEPURACIÓN: Imprimimos qué llega para saber por qué falla el correo
                            val usuarios = respuesta.body()
                            Log.d("LOGIN_DEBUG", "Usuarios recibidos: $usuarios")

                            val usuario = usuarios?.find {
                                // IMPORTANTE: Verifica si 'it.correo' es el nombre real de la variable en tu clase Usuario
                                it.correo.equals(correoIngresado, true) && it.passwordHash == passIngresado
                            }

                            if (usuario != null) {
                                prefs.edit().apply {
                                    putString("nombre", usuario.nombre)
                                    // Guardamos usando explícitamente el campo de la clase
                                    putString("email", usuario.correo)
                                    putString("extension", usuario.extension)
                                    putString("password", usuario.passwordHash)
                                    apply()
                                }

                                Log.d("LOGIN_DEBUG", "Sesión guardada para: ${usuario.correo}")
                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Error de servidor: ${respuesta.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnIngresar.isEnabled = true
                        btnIngresar.text = "INGRESAR"
                        Toast.makeText(this@LoginActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}