package com.example.uniconnect_iq;

import org.linphone.core.*;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

public class SipManager {
    private static final String TAG = "SipManager";
    private Core core;
    private static SipManager instance;
    private CallListener callListener;
    private RegistrationListener regListener;
    private final Context context;
    // Usamos el Handler del hilo principal para garantizar actualizaciones de UI seguras
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                // Postear al hilo principal
                if (callListener != null) {
                    mainHandler.post(() -> callListener.onCallStateChanged(call, state));
                }
            }

            @Override
            public void onAccountRegistrationStateChanged(Core core, Account account, RegistrationState state, String message) {
                Log.d(TAG, "ESTADO REGISTRO: " + state + " | MENSAJE: " + message);
                // Postear al hilo principal para actualizar el TextView sin errores
                if (regListener != null) {
                    mainHandler.post(() -> regListener.onRegistrationChanged(state, message));
                }
            }
        });

        core.start();

        // Bucle eficiente para procesar eventos
        new Thread(() -> {
            while (core != null) {
                synchronized (core) { core.iterate(); }
                try { Thread.sleep(20); } catch (InterruptedException e) { break; }
            }
        }).start();
    }

    public static synchronized SipManager getInstance(Context context) {
        if (instance == null) instance = new SipManager(context.getApplicationContext());
        return instance;
    }

    public void registrarExtension(String ext, String pass, String ip, String port) {
        synchronized (core) {
            core.clearAllAuthInfo();
            core.clearAccounts();

            AuthInfo authInfo = Factory.instance().createAuthInfo(ext, null, pass, null, "asterisk", ip);
            core.addAuthInfo(authInfo);

            AccountParams params = core.createAccountParams();
            params.setIdentityAddress(Factory.instance().createAddress("sip:" + ext + "@" + ip));
            params.setServerAddress(Factory.instance().createAddress("sip:" + ip + ":" + port));
            params.setRegisterEnabled(true);
            params.setTransport(TransportType.Udp);

            Account account = core.createAccount(params);
            core.addAccount(account);
            core.setDefaultAccount(account);
        }
    }

    public void llamar(String destino) {
        llamar(destino, "192.168.10.2");
    }

    public void llamar(String destino, String ipServidor) {
        synchronized (core) {
            if (core.getDefaultAccount() != null) {
                CallParams callParams = core.createCallParams(null);
                core.inviteAddressWithParams(Factory.instance().createAddress("sip:" + destino + "@" + ipServidor), callParams);
            }
        }
    }

    public void setAltavoz(boolean active) {
        synchronized (core) {
            for (AudioDevice device : core.getAudioDevices()) {
                if (active && device.getType() == AudioDevice.Type.Speaker) {
                    core.setOutputAudioDevice(device);
                    break;
                } else if (!active && device.getType() == AudioDevice.Type.Earpiece) {
                    core.setOutputAudioDevice(device);
                    break;
                }
            }
        }
    }

    public void colgar(Call call) {
        if (call != null) {
            synchronized (core) { call.terminate(); }
        }
    }
}