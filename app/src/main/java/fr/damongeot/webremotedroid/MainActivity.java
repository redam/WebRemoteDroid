package fr.damongeot.webremotedroid;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Network;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    public final static String LISTENING_PORT = "listen_port";
    public final static int LISTENING_PORT_DEF = 8888;
    public final static String START_AT_BOOT = "start_at_boot";
    public final static boolean START_AT_BOOT_DEF = false;
    public final static String HTTP_AUTHENTICATION = "http_authentication";
    public final static boolean HTTP_AUTHENTICATION_DEF = false;
    public final static String HTTP_AUTHENTICATION_USER = "http_user";
    public final static String HTTP_AUTHENTICATION_USER_DEF = "myuser";
    public final static String HTTP_AUTHENTICATION_PASSWORD = "http_password";
    public final static String HTTP_AUTHENTICATION_PASSWORD_DEF = "password";
    public final static String APP_LIST = "setAppList";
    public final static int PICK_APP_REQUEST = 1;
    private ArrayList<ApplicationInfo> arrayAppList;
    private ApplicationAdapter applicationAdapter;
    private SharedPreferences mSP;
    private Intent intentNLS = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // restore app list
        arrayAppList = new ArrayList<ApplicationInfo>();
        mSP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        for(String pkgName : mSP.getStringSet(APP_LIST,new HashSet<String>(0))) {
            try {
                ApplicationInfo app = this.getPackageManager().getApplicationInfo(pkgName, 0);
                arrayAppList.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        ToggleButton tb = (ToggleButton) findViewById(R.id.button_toggleservice);
        //set button current state
        tb.setChecked(isMyServiceRunning(NetworkListenService.class));

        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    Intent intentNLS = new Intent(MainActivity.this, NetworkListenService.class);
                    intentNLS.putExtra(LISTENING_PORT,getListeningPort());
                    startService(intentNLS);
                    Log.d(TAG,"Starting service");
                } else {
                    // The toggle is disabled
                    stopService(new Intent(MainActivity.this, NetworkListenService.class));
                    Log.d(TAG,"Stoping service");
                }
            }
        });

        Button bAddApp = (Button) findViewById(R.id.b_add_app);
        bAddApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SelectAppsActivity.class);
                startActivityForResult(intent, PICK_APP_REQUEST);
            }
        });

        final ListView listView = (ListView) findViewById(R.id.lv_app);
        applicationAdapter = new ApplicationAdapter(this,
                R.layout.list_row_application,
                arrayAppList);

        listView.setAdapter(applicationAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                ApplicationInfo applicationInfo = arrayAppList.get(i);

                //show info popup with ok/delete buttons
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.app_info))
                        .setMessage(getString(R.string.app_popup) + applicationInfo.packageName)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //delete app from list
                                arrayAppList.remove(i);
                                applicationAdapter.notifyDataSetChanged();
                                saveAppList();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });
    }

    /**
     * Show menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    /**
     * Handle menu item click
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.item_preferences:
                Intent i = new Intent(MainActivity.this, fr.damongeot.webremotedroid.PreferenceActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get listening port from shared preferences
     * @return
     */
    private int getListeningPort() {
        return Integer.parseInt(mSP.getString(LISTENING_PORT,LISTENING_PORT_DEF+""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PICK_APP_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                String pkgName = data.getStringExtra("packageName");
                Log.d(TAG,pkgName);
                try {
                    ApplicationInfo app = this.getPackageManager().getApplicationInfo(pkgName, 0);
                    arrayAppList.add(app);
                    applicationAdapter.notifyDataSetChanged();
                    saveAppList();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Save app list to "disk"
     */
    private void saveAppList() {
        //save app list into SharedPreferences
        HashSet<String> setAppList = new HashSet<String>();
        for(ApplicationInfo ai: arrayAppList) {
            setAppList.add(ai.packageName);
        }
        SharedPreferences.Editor editor = mSP.edit();
        editor.putStringSet(APP_LIST,setAppList);
        editor.commit();

    }
}
