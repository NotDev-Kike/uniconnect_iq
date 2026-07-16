package com.example.uniconnect_iq

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.core.Call
import org.linphone.core.RegistrationState

class DashboardActivity : AppCompatActivity(), SipManager.CallListener, SipManager.RegistrationListener {

    private var currentCall: Call? = null
    private lateinit var layoutLlamadaActiva: LinearLayout
    private lateinit var tvDashExtension: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. Inicializar Vistas
        val tvUserInfo = findViewById<TextView>(R.id.tvUserInfo)
        tvDashExtension = findViewById(R.id.tvDashExtension)
        layoutLlamadaActiva = findViewById(R.id.layoutLlamadaActiva)

        // 2. Cargar datos de sesión
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val nombre = prefs.getString("nombre", "Usuario") ?: "Usuario"
        val email = prefs.getString("email", "Sin correo") ?: "Sin correo"
        val ext = prefs.getString("extension", "000") ?: "000"

        tvUserInfo.text = nombre
        tvDashExtension.text = "Ext: $ext | $email | Estado: Conectando..."

        // 3. Inicializar SIP
        val sipManager = SipManager.getInstance(this)
        sipManager.setCallListener(this)
        sipManager.setRegistrationListener(this)

        if (ext != "000") {
            sipManager.registrarExtension(ext, prefs.getString("password", ""), "192.168.10.2", "5060")
        }

        // 4. Inicializar Radar pasando los datos que el servidor espera
        inicializarRadar(nombre, ext)

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onRegistrationChanged(state: RegistrationState, message: String?) {
        val statusText = when (state) {
            RegistrationState.Ok -> "OK"
            RegistrationState.Progress -> "En proceso..."
            RegistrationState.Failed -> "Fallido"
            RegistrationState.Cleared -> "Desconectado"
            else -> state.toString()
        }

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val ext = prefs.getString("extension", "000")
        val email = prefs.getString("email", "Sin correo")

        tvDashExtension.text = "Ext: $ext | $email | Estado: $statusText"
    }

    private fun inicializarRadar(nombre: String, ext: String) {
        val webView = findViewById<WebView>(R.id.webViewRadar)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("http://192.168.10.4:5000/")

        // El servidor Flask espera un campo llamado 'mac'.
        // Usamos el androidId como identificador único para el servidor.
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val nombreCompleto = "$nombre (Ext: $ext)"

        // Enviamos el objeto con las llaves 'mac' y 'nombreCompleto' que espera tu servidor Python
        val request = VinculacionRequest(androidId, nombreCompleto)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ApiService.crearParaRadar().vincularUsuario(request)
            } catch (e: Exception) {
                Log.e("RADAR_SYNC", "Error al vincular: ${e.message}")
            }
        }
    }

    override fun onCallStateChanged(call: Call, state: Call.State) {
        runOnUiThread {
            currentCall = call
            layoutLlamadaActiva.visibility = if (state == Call.State.IncomingReceived || state == Call.State.Connected) View.VISIBLE else View.GONE
        }
    }
}