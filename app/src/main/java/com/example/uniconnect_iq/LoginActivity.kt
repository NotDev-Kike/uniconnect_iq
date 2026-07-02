package com.example.uniconnect_iq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private val apiService = ApiService.crear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. VERIFICACIÓN DE SESIÓN: Si ya existe, saltar al Dashboard
        val prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        if (prefs.contains("extension")) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etLoginCorreo)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnIngresar = findViewById<Button>(R.id.btnLoginIngresar)
        val tvIrARegistro = findViewById<TextView>(R.id.tvIrARegistro) // Asegúrate de tener este ID en tu XML

        // 2. NAVEGACIÓN AL REGISTRO
        tvIrARegistro.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // 3. LÓGICA DE LOGIN
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuesta = apiService.obtenerUsuarios()
                    withContext(Dispatchers.Main) {
                        if (respuesta.isSuccessful) {
                            val usuario = respuesta.body()?.find {
                                it.correo.equals(correo, true) && it.passwordHash == pass
                            }

                            if (usuario != null) {
                                // GUARDAR SESIÓN
                                prefs.edit().apply {
                                    putString("extension", usuario.extension)
                                    putString("password", usuario.passwordHash)
                                    apply()
                                }

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
                        Toast.makeText(this@LoginActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}