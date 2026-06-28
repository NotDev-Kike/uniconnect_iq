package com.example.uniconnect_iq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Recuperamos los datos del usuario real logueado desde la MV
        val preferencias = getSharedPreferences("UniConnectPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = preferencias.getString("nombre_completo", "Usuario")
        val extensionSip = preferencias.getString("extension_sip", "No asignada")
        val idUsuario = preferencias.getInt("id_usuario", 0)

        // Enlace de componentes visuales
        val tvBienvenida = findViewById<TextView>(R.id.tvDashBienvenida)
        val tvExtension = findViewById<TextView>(R.id.tvDashExtension)
        val etDestinoVoip = findViewById<EditText>(R.id.etDestinoVoip)
        val btnLlamar = findViewById<Button>(R.id.btnLlamarVoip)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        // Inyectar datos en la pantalla
        tvBienvenida.text = "¡Hola, $nombreUsuario!"
        tvExtension.text = "Extensión Activa: [$extensionSip] • ID: $idUsuario"

        // ACCIÓN DE INTEGRACIÓN: Llamada VoIP (FreePBX)
        btnLlamar.setOnClickListener {
            val numeroDestino = etDestinoVoip.text.toString().trim()
            if (numeroDestino.isEmpty()) {
                Toast.makeText(this, "Escribe el número de extensión a marcar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Aquí se conectará el código Linphone/PJSIP del Compañero A contra su MV FreePBX
            Toast.makeText(this, "Marcando desde SIP/$extensionSip hacia SIP/$numeroDestino...", Toast.LENGTH_LONG).show()
        }

        // ACCIÓN: Limpiar sesión y regresar al Login
        btnCerrarSesion.setOnClickListener {
            val editor = preferencias.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}