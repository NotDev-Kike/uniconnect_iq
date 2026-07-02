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
    public interface RegistrationListener { void onRegistrationChanged(RegistrationState state); }

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
                Log.d(TAG, "ESTADO REGISTRO: " + state);
                if (regListener != null) regListener.onRegistrationChanged(state);
            }
        });

        core.start();
        new Thread(() -> {
            while (true) {
                synchronized (core) { core.iterate(); }
                try { Thread.sleep(20); } catch (Exception e) { break; }
            }
        }).start();
    }

    public static synchronized SipManager getInstance(Context context) {
        if (instance == null) instance = new SipManager(context.getApplicationContext());
        return instance;
    }

    public void registrarExtension(String ext, String pass, String ip, String port) {
        synchronized (core) {
            core.clearAllAuthInfo(); core.clearAccounts();
            AuthInfo authInfo = Factory.instance().createAuthInfo(ext, null, pass, null, null, ip);
            core.addAuthInfo(authInfo);
            AccountParams params = core.createAccountParams();
            params.setIdentityAddress(Factory.instance().createAddress("sip:" + ext + "@" + ip));
            params.setServerAddress(Factory.instance().createAddress("sip:" + ip + ":" + port + ";transport=udp"));
            params.setExpires(60);
            params.setRegisterEnabled(true);
            Account account = core.createAccount(params);
            core.addAccount(account);
            core.setDefaultAccount(account);
        }
    }

    // ESTE ES EL MÉTODO QUE FALTABA
    public void setAltavoz(boolean active) {
        synchronized (core) {
            AudioDevice[] devices = core.getAudioDevices();
            for (AudioDevice device : devices) {
                if (active && device.getType() == AudioDevice.Type.Speaker) {
                    core.setOutputAudioDevice(device);
                } else if (!active && device.getType() == AudioDevice.Type.Earpiece) {
                    core.setOutputAudioDevice(device);
                }
            }
        }
    }

    private void gestionarProximidad(Call.State state) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (state == Call.State.Connected) {
            wakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SipManager:Proximity");
            wakeLock.acquire(10*60*1000L);
        } else if (state == Call.State.End || state == Call.State.Released) {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        }
    }

    public void llamar(String destino) {
        synchronized (core) {
            if (core.getDefaultAccount() != null) {
                core.inviteAddress(Factory.instance().createAddress("sip:" + destino + "@" + core.getDefaultAccount().getParams().getServerAddress().getDomain()));
            }
        }
    }

    public void colgar(Call call) {
        if (call != null) {
            synchronized (core) { call.terminate(); }
        }
    }
}