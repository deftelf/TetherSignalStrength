package uk.co.deftelf.tethersignalstrength;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("app","Network connectivity change");
        
//        context.startService(new Intent(context, StrengthProviderService.class));

    }

}
