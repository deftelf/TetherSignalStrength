package uk.co.deftelf.tethersignalstrength;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.JSONStringer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class StrengthProviderService extends Service {

    static final int PORT = 31292;

    private boolean serve;

    private ServerSocket server;

    private int bars = -1;
    private int networkType = -1;

    private WifiManager wifii;

    private PhoneStateListener p;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        
        ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan.getActiveNetworkInfo() == null
                || (!conMan.getActiveNetworkInfo().isConnectedOrConnecting() && conMan.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        stop();

        wifii = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo d = wifii.getDhcpInfo();

        p = new PhoneStateListener() {

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                Log.d("", "sig: " + signalStrength.getGsmSignalStrength());
                bars = toBars(signalStrength.getGsmSignalStrength());
                
            }
            
            @Override
            public void onDataConnectionStateChanged(int state, int networkTypel) {
                super.onDataConnectionStateChanged(state, networkType);
                networkType = networkTypel;
            }
        };

        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(p, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        new Thread(new Runnable() {
            public void run() {
                try {
                    signalStrenthServerLoop();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            };
        }, "serverThread").start();
        
        Notification note = new Notification(R.drawable.ic_launcher, "Tether strength server", System.currentTimeMillis());
        PendingIntent click = PendingIntent.getActivity(this, 0, new Intent(this, Settings.class), 0);
        note.setLatestEventInfo(this, "Tether strength server", "Tether strength server", click);
        startForeground(1, note);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
        stopForeground(true);
        super.onDestroy();
    }

    private void stop() {

        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(p, PhoneStateListener.LISTEN_NONE);
        serve = false;
        try {
            if (server != null)
                server.close();
        } catch (IOException e) {
        }
    }

    private void signalStrenthServerLoop() throws Exception {
        serve = true;
        server = new ServerSocket(PORT);
        while (serve) {
            Socket sock = server.accept();
            OutputStream out = sock.getOutputStream();
            out.write(new JSONStringer().object().key("bars").value(bars).key("type").value(networkType).endObject().toString().getBytes("UTF-8"));
            out.close();
        }
    }

    public static int toBars(int signalStrength) {
        switch (signalStrength) {
        case 0:
        case 1:
        case 2:
            return 0;
        case 3:
        case 4:
            return 1;
        case 5:
        case 6:
        case 7:
            return 2;
        case 8:
        case 9:
        case 10:
            return 3;
        case 99:
            return -1;
        default:
            return 4;
        }
    }

}
