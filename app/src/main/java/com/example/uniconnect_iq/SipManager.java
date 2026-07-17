package com.example.uniconnect_iq;

import org.linphone.core.*;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class SipManager {
    private static final String TAG = "SipManager";
    private Core core;
    private static SipManager instance;
    private CallListener callListener;
    private RegistrationListener regListener;
    private PowerManager.WakeLock wakeLock;
    private Context context;

    public interface CallListener { void onCallStateChanged(Call call, Call.State state); }
    public interface RegistrationListener { void onRegistrationChanged(RegistrationState state, String message); }

    public void setCallListener(CallListener l) { this.callListener = l; }
    public void setRegistrationListener(RegistrationListener l) { this.regListener = l; }

    private SipManager(Context context) {
        this.context = context.getApplicationContext();
        core = Factory.instance().createCore(null, null, context);

        core.addListener(new CoreListenerStub() {
            @Override
            public void onCallStateChanged(Core core, Call call, Call.State state, String message) {
                gestionarProximidad(state);
                if (callListener != null) callListener.onCallStateChanged(call, state);
            }

            @Override
            public void onAccountRegistrationStateChanged(Core core, Account account, RegistrationState state, String message) {
                Log.e(TAG, "ESTADO REGISTRO: " + state + " | MENSAJE: " + message);
                if (regListener != null) regListener.onRegistrationChanged(state, message);
            }
        });

        core.start();

        // HILO DE ITERACIÓN (SIN SYNCHRONIZED para evitar deadlocks/bloqueos)
        new Thread(() -> {
            while (core != null) {
                core.iterate();
                try { Thread.sleep(20); } catch (Exception e) { break; }
            }
        }).start();
    }

    public static synchronized SipManager getInstance(Context context) {
        if (instance == null) instance = new SipManager(context.getApplicationContext());
        return instance;
    }

    public void registrarExtension(String ext, String pass, String ip, String port) {
        new Thread(() -> {
            core.clearAllAuthInfo();
            core.clearAccounts();

            // 1. AuthInfo: Ajustado para coincidir con tu [auth1004] en pjsip.conf
            // El realm debe ser el nombre del host o null si asterisk lo maneja por defecto
            AuthInfo authInfo = Factory.instance().createAuthInfo(ext, null, pass, null, null, ip);
            core.addAuthInfo(authInfo);

            // 2. AccountParams: Alineado con tu configuración de NAT
            AccountParams params = core.createAccountParams();
            params.setIdentityAddress(Factory.instance().createAddress("sip:" + ext + "@" + ip));
            params.setServerAddress(Factory.instance().createAddress("sip:" + ip + ":" + port));
            params.setRegisterEnabled(true);
            params.setTransport(TransportType.Udp);

            // Parámetros críticos para que Asterisk no lance el error AOR
            params.setOutboundProxyEnabled(false);
            params.setExpires(300);

            Account account = core.createAccount(params);
            core.addAccount(account);
            core.setDefaultAccount(account);

            Log.d(TAG, "Registro solicitado para: " + ext);
        }).start();
    }

    public void llamar(String destino) {
        llamar(destino, "192.168.10.2");
    }

    public void llamar(String destino, String ipServidor) {
        new Thread(() -> {
            if (core.getDefaultAccount() != null) {
                core.inviteAddress(Factory.instance().createAddress("sip:" + destino + "@" + ipServidor));
            }
        }).start();
    }

    public void setAltavoz(boolean active) {
        AudioDevice[] devices = core.getAudioDevices();
        for (AudioDevice device : devices) {
            if (active && device.getType() == AudioDevice.Type.Speaker) {
                core.setOutputAudioDevice(device);
            } else if (!active && device.getType() == AudioDevice.Type.Earpiece) {
                core.setOutputAudioDevice(device);
            }
        }
    }

    private void gestionarProximidad(Call.State state) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (state == Call.State.Connected) {
            if (wakeLock == null) wakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SipManager:Proximity");
            if (!wakeLock.isHeld()) wakeLock.acquire(600000L);
        } else if (state == Call.State.End || state == Call.State.Released) {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        }
    }

    public void colgar(Call call) {
        if (call != null) {
            new Thread(call::terminate).start();
        }
    }
}