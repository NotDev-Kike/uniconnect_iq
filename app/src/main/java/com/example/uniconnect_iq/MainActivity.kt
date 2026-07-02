package com.example.uniconnect_iq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val apiService = ApiService.crear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlace de vistas
        val etIdUsuario = findViewById<EditText>(R.id.etIdUsuario)
        val etNombreUsuario = findViewById<EditText>(R.id.etNombreUsuario)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etCarrera = findViewById<EditText>(R.id.etCarrera)
        val etIdRol = findViewById<EditText>(R.id.etIdRol)
        val etExtensionVoip = findViewById<EditText>(R.id.etExtensionVoip)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEstado = findViewById<TextView>(R.id.tvEstadoRegistro)

        btnGuardar.setOnClickListener {
            // Validaciones
            val idTxt = etIdUsuario.text.toString().trim()
            val nomTxt = etNombreUsuario.text.toString().trim()
            val apeTxt = etApellido.text.toString().trim()
            val corrTxt = etCorreo.text.toString().trim()
            val passTxt = etPassword.text.toString().trim()
            val carTxt = etCarrera.text.toString().trim()
            val rolTxt = etIdRol.text.toString().trim()
            val extTxt = etExtensionVoip.text.toString().trim()

            if (idTxt.isEmpty() || nomTxt.isEmpty() || apeTxt.isEmpty() ||
                corrTxt.isEmpty() || passTxt.isEmpty() || carTxt.isEmpty() ||
                rolTxt.isEmpty() || extTxt.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoRegistro = UsuarioRequest(
                idUsuario = idTxt.toInt(),
                nombre = nomTxt,
                apellido = apeTxt,
                correo = corrTxt,
                passwordHash = passTxt,
                carrera = carTxt,
                idRol = rolTxt.toInt(),
                extension = extTxt,
                estado = "activo"
            )

            progressBar.visibility = View.VISIBLE
            tvEstado.text = "Registrando..."
            btnGuardar.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuestaInsert = apiService.registrarUsuario(nuevoRegistro)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true

                        if (respuestaInsert.isSuccessful) {
                            tvEstado.text = "¡Registro exitoso! Redirigiendo..."
                            Toast.makeText(this@MainActivity, "Usuario creado correctamente", Toast.LENGTH_LONG).show()

                            // Redirección al Login tras éxito
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Cerramos esta actividad
                        } else {
                            tvEstado.text = "Error: Verifica datos (Código: ${respuestaInsert.code()})"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true
                        tvEstado.text = "Error de conexión: ${e.message}"
                    }
                }
            }
        }
    }
}