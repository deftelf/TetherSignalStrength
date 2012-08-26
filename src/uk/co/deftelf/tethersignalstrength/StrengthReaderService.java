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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class StrengthReaderService extends Service {

    private WifiManager wifii;

    private BroadcastReceiver screenRx;

    private IntentFilter filter;

    private NotificationManager noteMan;

    private Notification note;

    private UpdateThread checker;

    private Socket getSock;

    private PendingIntent click;

    class UpdateThread extends Thread {
        boolean check = true;

        public UpdateThread() {
            setName("checkerThread");
        }

        public void run() {
            while (check) {

                DhcpInfo d = wifii.getDhcpInfo();
                try {

                    Thread.sleep(3000);
                    getSock = new Socket();
                    getSock.connect(new InetSocketAddress(intToIp(d.gateway), StrengthProviderService.PORT), 2000);
                    InputStream is = getSock.getInputStream();
                    String stuff = new Scanner(is).useDelimiter("\\A").next();
                    JSONObject info = new JSONObject(stuff);
                    int gotBars = info.getInt("bars");
                    int type = info.getInt("type");

                    putNotif(gotBars, type);

                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (check) {
                        putNotif(-1, -1);
                    }
                }

            }
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

        wifii = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        screenRx = new BroadcastReceiver() {

            private boolean screenOn = true;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    screenOn = true;
                }
                else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    screenOn = false;
                }
                
                if (screenOn && wifii.getConnectionInfo() != null && wifii.isWifiEnabled() && wifii.getConnectionInfo().getNetworkId() != -1
                        ) {
                    startUpdater();
                } else {
                    stopUpdater();
                }

            }

        };

        filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        noteMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        note = new Notification(R.drawable.ic_launcher, "", 0);
        click = PendingIntent.getActivity(this, 0, new Intent(this, Settings.class), 0);
        note.setLatestEventInfo(this, "Tethered signal strength", "Waiting", click);
        note.defaults = 0;
        
        startForeground(1, note);

        // note.priority =
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            unregisterReceiver(screenRx);
        } catch (IllegalArgumentException e) {
        }
        registerReceiver(screenRx, filter);

        startUpdater();
        
        Log.d(this.getClass().toString(), "onStartCommand " + intent + " " + flags);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().toString(), "onDestroy ");
        stopUpdater();
        stopForeground(true);
        unregisterReceiver(screenRx);
        super.onDestroy();
    }

    private void stopUpdater() {
        if (checker != null) {
            checker.check = false;
            checker.interrupt();
            if (getSock != null)
                try {
                    getSock.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
    }

    private void startUpdater() {
        stopUpdater();
        checker = new UpdateThread();
        checker.start();
        noteMan.notify(1, note);
    }

    private void putNotif(int gotBars, int type) {
        int icon;
        switch (gotBars) {
        case 1:
            icon = R.drawable.ic_bar1;
            break;
        case 2:
            icon = R.drawable.ic_bar2;
            break;
        case 3:
            icon = R.drawable.ic_bar3;
            break;
        case 4:
            icon = R.drawable.ic_bar4;
            break;
        case 0:
            icon = R.drawable.ic_bar0;
            break;
        default:
            icon = R.drawable.ic_launcher;
            break;
        }
        
        String typeName;
        switch (type) {
        case TelephonyManager.NETWORK_TYPE_1xRTT:
            typeName = "1xRTT";
            break; // ~ 50-100 kbps // ~ 50-100 kbps
        case TelephonyManager.NETWORK_TYPE_CDMA:
            typeName = "CDMA";
            break; // ~ 50-100 kbps // ~ 14-64 kbps
        case TelephonyManager.NETWORK_TYPE_EDGE:
            typeName = "EDGE";
            break; // ~ 50-100 kbps
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
            typeName = "EVDO";
            break; // ~ 50-100 kbps
        case TelephonyManager.NETWORK_TYPE_GPRS:
            typeName = "GPRS";
            break;
        case TelephonyManager.NETWORK_TYPE_HSDPA:
            typeName = "HSDPA";
            break;// ~ 2-14 Mbps
        case TelephonyManager.NETWORK_TYPE_HSPA:
            typeName = "HSPA";
            break;// ~ 700-1700 kbps
        case TelephonyManager.NETWORK_TYPE_HSUPA:
            typeName = "HSUPA";
            break; // ~ 1-23 Mbps
        case TelephonyManager.NETWORK_TYPE_UMTS:
            typeName = "UMTS";
            break; // ~ 400-7000 kbps
        // NOT AVAILABLE YET IN API LEVEL 7
        case TelephonyManager.NETWORK_TYPE_EHRPD:
            typeName = "EHRPD";
            break; // ~ 1-2 Mbps
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
            typeName = "EVDO-B";
            break; // ~ 5 Mbps
        case TelephonyManager.NETWORK_TYPE_HSPAP:
            typeName = "HSPAP";
            break; // ~ 10-20 Mbps
        case TelephonyManager.NETWORK_TYPE_IDEN:
            typeName = "IDEN";
            break; // ~25 kbps 
        case TelephonyManager.NETWORK_TYPE_LTE:
            typeName = "LTE";
            break; // ~ 10+ Mbps
        default:
            typeName = "Unknown";
        }

        String message = "Tethered phone bars=" + gotBars + "/4 on " + typeName;
        note.when = System.currentTimeMillis();
        note.setLatestEventInfo(this, "Tethered signal strength", message, click);
        note.icon = icon;
        noteMan.notify(1, note);
    }

    public String intToIp(int i) {

        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    public int getBars() {
        DhcpInfo d = wifii.getDhcpInfo();
        InputStream is = null;
        try {

            Socket getSock = new Socket();
            getSock.connect(new InetSocketAddress(intToIp(d.gateway), StrengthProviderService.PORT), 2000);
            is = getSock.getInputStream();
            String stuff = new Scanner(is).useDelimiter("\\A").next();
            JSONObject info = new JSONObject(stuff);
            final int gotBars = info.getInt("bars");
            return gotBars;
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return -1;
    }
}
