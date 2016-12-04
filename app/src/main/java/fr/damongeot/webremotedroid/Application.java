package fr.damongeot.webremotedroid;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;

/**
 * Created by regis on 04/12/16.
 */

public class Application {
    private static String TAG = "Application";
    private SharedPreferences mSP;
    private ActivityManager am;
    private Context ctx;

    public Application(Context context) {
        mSP = PreferenceManager.getDefaultSharedPreferences(context);
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ctx=context;
    }

    /**
     * Start app from package name if user has added it to launchable app list
     */
    public void launch(String packageName) throws Exception {
        for(String pkgName : mSP.getStringSet(MainActivity.APP_LIST,new HashSet<String>(0))) {
            if(packageName.equals(pkgName)) {
                //launch app
                Log.d(TAG,"Starting app " + packageName);
                Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(pkgName);
                KeyguardManager km = (KeyguardManager) ctx.getSystemService(KEYGUARD_SERVICE);
                final KeyguardManager.KeyguardLock kl=km.newKeyguardLock("WebRemoteDroid");
                kl.disableKeyguard();

                PowerManager pm = (PowerManager) ctx.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wl=pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, TAG);
                wl.acquire();
                ctx.startActivity(intent);

                //let the app have some time to start before removing wakelock and keyguard
                Thread.sleep(5000);
                wl.release();
                kl.reenableKeyguard();

                return;
            }
        }

        throw new Exception("App "+packageName+" is not authorized to be started remotely (or is an invalid package name)");
    }

    private int findPIDbyPackageName(String packagename) {
        int result = -1;

        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo pi : am.getRunningAppProcesses()){
                if (pi.processName.equalsIgnoreCase(packagename)) {
                    result = pi.pid;
                }
                if (result != -1) break;
            }
        } else {
            result = -1;
        }

        return result;
    }

    private boolean isPackageRunning(String packagename) {
        return findPIDbyPackageName(packagename) != -1;
    }

    /**
     * Kill processes from a given package name
     * @param packagename
     * @return
     */
    public boolean killPackageProcesses(String packagename) {
        boolean result = false;

        if (am != null) {
            am.killBackgroundProcesses(packagename);
            result = !isPackageRunning(packagename);
        } else {
            result = false;
        }

        return result;
    }
}
