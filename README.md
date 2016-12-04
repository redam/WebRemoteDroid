# Web Remote Droid

Control your smartphone remotely using HTTP GET requests.

--- Apps
Launch apps on your android phone remotely using HTTP GET requests.

For example, to launch firefox, use your browser to request :
```
http://yourPhoneIP:8888/app/start/org.mozilla.firefox
```

The app need to be requested by its package name (Web Remote Droid will show the package name of each app on your phone, don't worry).

To stop the app remotely, you just need to switch from "start" to "stop" in the URL :
```
http://yourPhoneIP:8888/app/stop/org.mozilla.firefox
```

Be aware that android doesn't allow a foreground app to be killed, only background processes. If you remotely launch an app that stays in foreground, it can't be killed remotely by Remote App Launch.

Each app you want to start remotely need to be added in a white list within Web Remote Droid (for security reasons). You can add one more security layer by using HTTP Authentication with a user/password needed to start/stop apps. In that case, to launch an app just use the following URL :
```
http://username:password@yourPhoneIP:8888/app/start/org.mozilla.firefox
```
