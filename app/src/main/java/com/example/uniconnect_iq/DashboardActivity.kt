package com.example.uniconnect_iq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.Call
import org.linphone.core.RegistrationState

class DashboardActivity : AppCompatActivity(), SipManager.CallListener, SipManager.RegistrationListener {

    private var currentCall: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val sipManager = SipManager.getInstance(this)
        sipManager.setCallListener(this)
        sipManager.setRegistrationListener(this)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val ext = prefs.getString("extension", "") ?: ""
        val pass = prefs.getString("password", "") ?: ""

        findViewById<TextView>(R.id.tvUserInfo).text = "Usuario Extensión: $ext"

        if (ext.isNotEmpty()) {
            sipManager.registrarExtension(ext, pass, "192.168.1.27", "5060")
        }

        // Botón Llamar
        findViewById<Button>(R.id.btnLlamarVoip).setOnClickListener {
            val num = findViewById<EditText>(R.id.etDestinoVoip).text.toString()
            if (num.isNotEmpty()) sipManager.llamar(num)
        }

        // BOTÓN CERRAR SESIÓN (Corregido)
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onRegistrationChanged(state: RegistrationState) {
        runOnUiThread {
            findViewById<TextView>(R.id.tvDashExtension).text = "Estado: $state"
        }
    }

    override fun onCallStateChanged(call: Call, state: Call.State) {
        runOnUiThread {
            currentCall = call
            when (state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> mostrarInterfaz(true, true)
                Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.Connected -> mostrarInterfaz(true, false)
                Call.State.End, Call.State.Released, Call.State.Error -> {
                    currentCall = null
                    mostrarInterfaz(false, false)
                }
                else -> {}
            }
        }
    }

    private fun mostrarInterfaz(visible: Boolean, esEntrante: Boolean) {
        val layout = findViewById<LinearLayout>(R.id.layoutLlamadaActiva)
        val btnContestar = findViewById<Button>(R.id.btnContestar)

        layout.post {
            layout.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) layout.bringToFront()
        }

        btnContestar.visibility = if (esEntrante) View.VISIBLE else View.GONE

        // Usamos setOnClickListener asegurando limpiar acciones previas
        btnContestar.setOnClickListener {
            currentCall?.accept()
            btnContestar.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnColgar).setOnClickListener {
            currentCall?.terminate()
            mostrarInterfaz(false, false)
        }

        findViewById<Button>(R.id.btnAltavoz).setOnClickListener {
            SipManager.getInstance(this).setAltavoz(true)
            Toast.makeText(this, "Altavoz activado", Toast.LENGTH_SHORT).show()
        }
    }
}