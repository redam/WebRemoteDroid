package fr.damongeot.webremotedroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StartAtBootReceiver extends BroadcastReceiver {
    public StartAtBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            //start listening service if user checked option
            SharedPreferences mSP = PreferenceManager.getDefaultSharedPreferences(context);
            if(mSP.getBoolean(MainActivity.START_AT_BOOT,MainActivity.START_AT_BOOT_DEF)) {
                Intent serviceIntent = new Intent(context, NetworkListenService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
