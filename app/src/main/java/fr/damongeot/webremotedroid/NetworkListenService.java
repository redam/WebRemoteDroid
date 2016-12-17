package fr.damongeot.webremotedroid;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class NetworkListenService extends Service {
    private final static String TAG = "NetworkListenService";
    private final static int ACTION_NO_OP = 0,
            ACTION_START_APP = 1,
            ACTION_STOP_APP = 2,
            ACTION_FLASH_ON = 3,
            ACTION_FLASH_OFF = 4,
            ACTION_GET = 5;
    private String getString; //in case of GET request, what page requested

    private int mPort;
    private ServerSocket mServerSocket;
    private boolean mIsRunning; //server is running
    private SharedPreferences mSP;
    private boolean httpAuth; //is http authentication enabled ?
    private String httpUsername,httpPassword;
    private ActivityManager am;
    private AssetManager assetManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mIsRunning) { //start only if not already running
            assetManager = getAssets();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mSP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            httpAuth = mSP.getBoolean(MainActivity.HTTP_AUTHENTICATION, MainActivity.HTTP_AUTHENTICATION_DEF);
            httpUsername = mSP.getString(MainActivity.HTTP_AUTHENTICATION_USER, MainActivity.HTTP_AUTHENTICATION_USER_DEF);
            httpPassword = mSP.getString(MainActivity.HTTP_AUTHENTICATION_PASSWORD, MainActivity.HTTP_AUTHENTICATION_PASSWORD_DEF);
            mPort = intent.getIntExtra(MainActivity.LISTENING_PORT, MainActivity.LISTENING_PORT_DEF);
            mIsRunning = true;
            am = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
            new ListenOnTCP().execute();
        }

        return Service.START_REDELIVER_INTENT;
    }

    // listen on tcp socket for incoming connections
    private class ListenOnTCP extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                mServerSocket = new ServerSocket(mPort);
                mServerSocket.setSoTimeout(5000); //set a timeout to be able to regularly check for terminating the service
                Log.d(TAG,"Listening on port "+mPort);

                while (mIsRunning) {
                    try {
                        Socket socket = mServerSocket.accept();
                        handleRequest(socket);
                        socket.close();
                    } catch(SocketTimeoutException e) {
                        //do nothing
                    }
                }

                mServerSocket.close();
            } catch (SocketException e) {
                // The server was stopped; ignore.
                Log.d(TAG,e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Web server error.", e);
            }

            return null;
        }

    }

    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */
    private void handleRequest(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        String packageName = null;
        boolean foundAuthHeader = false;
        boolean authSucceed = false;
        int action = ACTION_NO_OP;
        String outputMessage = "";

        try {
            // Read HTTP headers
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            String line;

            //pattern for auth
            Pattern pAuth = Pattern.compile("Authorization: Basic (.*)"); //match Authorization header

            //pattern for start/stop app
            Pattern pApp = Pattern.compile("GET /app/(start|stop)/([^ ]*).*");

            //pattern for flash
            Pattern pFlash = Pattern.compile("GET /flash/(on|off) .*");

            //pattern for any other GET request
            Pattern pGet = Pattern.compile("GET /(.*) .*");


            while (!TextUtils.isEmpty(line = reader.readLine())) {

                //check for http authentication header
                Matcher m = pAuth.matcher(line);
                if (m.matches()) {
                    //Log.d(TAG,"found authorization header : "+line);
                    authSucceed = checkAuth(m.group(1));
                }

                //is it an app start/stop request
                m = pApp.matcher(line);
                if (m.matches()) {
                    //Log.d(TAG, m.group(2));
                    packageName = m.group(2);
                    action = m.group(1).equals("start") ? ACTION_START_APP : ACTION_STOP_APP;
                    continue;
                }

                //is it a flash request
                m = pFlash.matcher(line);
                if (m.matches()) {
                    action = m.group(1).equals("on") ? ACTION_FLASH_ON : ACTION_FLASH_OFF;
                    continue;
                }

                m = pGet.matcher(line);
                if (m.matches()) {
                    action = ACTION_GET;
                    getString = m.group(1);
                    continue;
                }

                //Log.d(TAG,"Unknow line : "+line);
            }

            // GET header arrives before authorization so we cant tread it in while loop
            if (!httpAuth || authSucceed) {
                Application app;
                fr.damongeot.webremotedroid.Camera cam;

                switch (action) {
                    case ACTION_START_APP:
                        app = new Application(getBaseContext());
                        try {
                            app.launch(packageName);
                            outputMessage = "App launched";
                        } catch (Exception e) {
                            outputMessage = e.getMessage();
                        }
                        break;
                    case ACTION_STOP_APP:
                        app = new Application(getBaseContext());
                        app.killPackageProcesses(packageName);
                        outputMessage = "App stopped";
                        break;
                    case ACTION_FLASH_ON:
                        cam = new fr.damongeot.webremotedroid.Camera(getBaseContext());
                        try {
                            cam.setFlash(true);
                        } catch (Exception e) {
                            outputMessage = e.getMessage();
                        }
                        break;
                    case ACTION_FLASH_OFF:
                        cam = new fr.damongeot.webremotedroid.Camera(getBaseContext());
                        try {
                            cam.setFlash(false);
                        } catch (Exception e) {
                            outputMessage = e.getMessage();
                        }
                        break;
                    case ACTION_GET:
                        try {
                            handleGetAssetRequest(getString, output);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        outputMessage = "Invalid request";
                        break;
                }

            }


            // Send out the content.
            //if no authorization sent while http auth enabled, send auth headers
            if (httpAuth && !authSucceed) {
                output.println("HTTP/1.0 401 Unauthorized");
                output.println("WWW-Authenticate: Basic realm=\"Web Remote Droid\"");
            } else if (action != ACTION_GET) {
                output.println("HTTP/1.0 200 OK");
                output.println("");
                output.println(outputMessage);
            }
            output.flush();
        } finally {
            if (null != output) {
                output.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }

    /**
     * Return asset matching get request
     * @param getString
     * @return
     */
    private void handleGetAssetRequest(String getString,PrintStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int count;

        if(getString.isEmpty()) getString = "index.html";
        BufferedInputStream bis = new BufferedInputStream(assetManager.open(getString));

        output.println("HTTP/1.0 200 OK");
        output.println("");
        while((count = bis.read(buffer,0,4096)) != -1) {
            output.write(buffer,0,count);
        }
        output.flush();
    }

    /**
     * Check if username/password is matching from HTTP Basic header (user:password encoded in base64)
     * @param authHeader
     * @return
     */
    private boolean checkAuth(String authHeader) {
        String userpass = new String(Base64.decode(authHeader.getBytes(),Base64.DEFAULT));
        //Log.d(TAG,"HTTP Request contains authorization header : "+userpass);
        if(userpass.equals(httpUsername+":"+httpPassword)) {
            return true;
        } else {
            Log.w(TAG,"User/password did not match : "+userpass);
            return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        //stop listening on next socket timeout
        mIsRunning=false;
    }

}
