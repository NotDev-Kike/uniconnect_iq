package com.example.uniconnect_iq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.Call
import org.linphone.core.RegistrationState

class DashboardActivity : AppCompatActivity(), SipManager.CallListener, SipManager.RegistrationListener {

    private var currentCall: Call? = null
    private val apiService = ApiService.crear()
    private val radarApiService = RadarApiService.crear()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) iniciarReconocimientoVoz()
        else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
    }

    private val launcherIA = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val frase = data?.get(0)?.lowercase() ?: ""

        val numeroRegex = "\\d+".toRegex()
        val match = numeroRegex.find(frase)

        if (match != null) {
            val numero = match.value
            Toast.makeText(this, "Marcando extensión: $numero", Toast.LENGTH_SHORT).show()
            SipManager.getInstance(this).llamar(numero)
        } else if (frase.isNotEmpty()) {
            Toast.makeText(this, "IA buscando destino: $frase", Toast.LENGTH_SHORT).show()
            enviarLlamadaIA(frase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ✅ SIP - IGUAL QUE ANTES
        val sipManager = SipManager.getInstance(this)
        sipManager.setCallListener(this)
        sipManager.setRegistrationListener(this)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val ext = prefs.getString("extension", "") ?: ""
        val pass = prefs.getString("password", "") ?: ""
        val nombre = prefs.getString("nombre", "") ?: ""
        val apellido = prefs.getString("apellido", "") ?: ""

        findViewById<TextView>(R.id.tvUserInfo).text = "Usuario Extensión: $ext"

        // ✅ REGISTRO SIP - IGUAL QUE ANTES
        if (ext.isNotEmpty()) {
            sipManager.registrarExtension(ext, pass, "192.168.10.2", "5060")
        }

        // ✅ BOTONES - IGUAL QUE ANTES
        findViewById<Button>(R.id.btnLlamarVoip).setOnClickListener {
            val num = findViewById<EditText>(R.id.etDestinoVoip).text.toString()
            if (num.isNotEmpty()) sipManager.llamar(num)
        }

        findViewById<Button>(R.id.btnActivarIA).setOnClickListener {
            verificarPermisosYLanzarIA()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnContestar).setOnClickListener {
            currentCall?.accept()
            findViewById<Button>(R.id.btnContestar).visibility = View.GONE
        }

        findViewById<Button>(R.id.btnColgar).setOnClickListener {
            currentCall?.terminate()
            mostrarInterfaz(false, false)
        }

        findViewById<Button>(R.id.btnAltavoz).setOnClickListener {
            SipManager.getInstance(this).setAltavoz(true)
            Toast.makeText(this, "Altavoz activado", Toast.LENGTH_SHORT).show()
        }

        // ==================== WEBVIEW PARA RADAR ====================
        val webView = findViewById<WebView>(R.id.webViewRadar)
        configurarWebView(webView, ext, "$nombre $apellido".trim())
    }

    // ==================== SIP LISTENERS ====================

    override fun onRegistrationChanged(state: RegistrationState, message: String?) {
        runOnUiThread {
            findViewById<TextView>(R.id.tvDashExtension).text = "Estado: $state"
            if (state == RegistrationState.Failed) {
                Log.e("Dashboard", "Error de registro: $message")
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            }
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

    // ==================== IA ====================

    private fun verificarPermisosYLanzarIA() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            iniciarReconocimientoVoz()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun iniciarReconocimientoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre del contacto")
        }
        launcherIA.launch(intent)
    }

    private fun enviarLlamadaIA(fraseBruta: String) {
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val extOrigen = prefs.getString("extension", "") ?: ""

        var fraseLimpia = fraseBruta
            .replace("llamar a", "", ignoreCase = true)
            .replace("comunícame con", "", ignoreCase = true)
            .replace("extensión", "", ignoreCase = true)
            .replace("con la", "", ignoreCase = true)
            .trim()

        if (fraseLimpia.contains("siento") || fraseLimpia.contains("mil")) {
            fraseLimpia = fraseLimpia.replace("siento", "100").replace("mil", "1000")
                .replace("dos", "2").replace("uno", "1").replace("tres", "3")
                .replace(Regex("\\D"), "")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = LlamadaRequest(fraseLimpia, extOrigen)
                val response = apiService.iniciarLlamadaIA(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DashboardActivity, "Llamando a: $fraseLimpia", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@DashboardActivity, "Error de servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e("IA_ERROR", "Error al enviar a la API", e)
                }
            }
        }
    }

    private fun mostrarInterfaz(visible: Boolean, esEntrante: Boolean) {
        val layout = findViewById<LinearLayout>(R.id.layoutLlamadaActiva)
        val btnContestar = findViewById<Button>(R.id.btnContestar)
        layout.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) layout.bringToFront()
        btnContestar.visibility = if (esEntrante) View.VISIBLE else View.GONE
    }

    // ==================== WEBVIEW RADAR ====================

    private fun configurarWebView(webView: WebView, extension: String, nombre: String) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    enviarIdentificacionAlRadar(extension, nombre)
                }
            }

            loadUrl("http://192.168.10.4:5000/")
        }
    }

    private fun enviarIdentificacionAlRadar(extension: String, nombre: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val androidId =
                    DeviceIdentifier.getAndroidId(
                        this@DashboardActivity
                    )


                val dispositivo =
                    DeviceIdentifier.getDeviceName()

                val request = VinculacionRequest(

                    extension = extension,

                    nombre = nombre,

                    dispositivo = dispositivo,

                    token = androidId

                )


                Log.d(
                    "RADAR",
                    "Enviando Android ID: $androidId Usuario: $nombre"
                )


                val response = radarApiService.vincularUsuario(request)


                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {

                        Log.d(
                            "RADAR",
                            "✅ Identificación enviada correctamente"
                        )

                    } else {

                        Log.e(
                            "RADAR",
                            "❌ Error HTTP: ${response.code()}"
                        )

                    }

                }


            } catch (e: Exception) {

                Log.e(
                    "RADAR",
                    "Error al enviar identificación",
                    e
                )

            }
        }
    }
}