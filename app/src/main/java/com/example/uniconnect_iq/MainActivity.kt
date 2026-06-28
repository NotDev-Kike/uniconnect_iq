package com.example.uniconnect_iq

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.uniconnect_iq.R

class MainActivity : AppCompatActivity() {

    private val apiService = ApiService.crear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlace de los 8 campos dinámicos y reales del XML
        val etIdUsuario = findViewById<EditText>(R.id.etIdUsuario)
        val etNombreUsuario = findViewById<EditText>(R.id.etNombreUsuario)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etCarrera = findViewById<EditText>(R.id.etCarrera)
        val etIdRol = findViewById<EditText>(R.id.etIdRol)
        val etExtensionVoip = findViewById<EditText>(R.id.etExtensionVoip)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnSincronizar = findViewById<Button>(R.id.btnSincronizar)
        val tvTablaContenido = findViewById<TextView>(R.id.tvTablaContenido)

        fun obtenerListaUsuarios() {
            tvTablaContenido.text = "Sincronizando..."
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuesta = apiService.obtenerUsuarios()
                    withContext(Dispatchers.Main) {
                        if (respuesta.isSuccessful && respuesta.body() != null) {
                            val lista = respuesta.body()!!
                            if (lista.isEmpty()) {
                                tvTablaContenido.text = "No hay registros en la base de datos."
                            } else {
                                val constructorTexto = StringBuilder()
                                for (user in lista) {
                                    constructorTexto.append("ID: ${user.idUsuario} | ${user.nombre} ${user.apellido}\n")
                                    constructorTexto.append("CORREO: ${user.correo} | SIP: ${user.extension}\n")
                                    constructorTexto.append("===============================\n\n")
                                }
                                tvTablaContenido.text = constructorTexto.toString()
                            }
                        } else {
                            tvTablaContenido.text = "Error devuelto por la API: Code ${respuesta.code()}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvTablaContenido.text = "Error de red: ${e.localizedMessage}"
                    }
                }
            }
        }

        btnGuardar.setOnClickListener {
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
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
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

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuestaInsert = apiService.registrarUsuario(nuevoRegistro)
                    withContext(Dispatchers.Main) {
                        if (respuestaInsert.isSuccessful) {
                            Toast.makeText(this@MainActivity, "¡Registrado con éxito! Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()

                            // PASO CLAVE: Cerramos esta actividad para regresar al Login de forma limpia
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Fallo SQL: Revisa si el ID o Correo ya existen (Código: ${respuestaInsert.code()})", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error de envío: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnSincronizar.setOnClickListener { obtenerListaUsuarios() }
        obtenerListaUsuarios()
    }
}