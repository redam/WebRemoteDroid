package fr.damongeot.webremotedroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

/**
 * Created by regis on 04/12/16.
 */

public class Camera {
    private static String TAG = "Camera";

    private static android.hardware.Camera mCamera;
    private static CameraManager mCamManager;
    private static android.hardware.Camera.Parameters params;
    private static boolean isFlashOn=false;
    private Context ctx;
    private SurfaceTexture mPreviewTexture;

    public Camera(Context context) {
        ctx=context;
    }

    /**
     * Turn on or off flash
     * @param state true = flash on, false = flash off
     */
    @SuppressLint("NewApi")
    public void setFlash(boolean state) throws Exception {
        if(isFlashOn == state) return;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //use camera1 API before LOLLIPOP

            //get camera parameters if not already done
            if (mCamera == null) {
                try {
                    mCamera = android.hardware.Camera.open();
                    params = mCamera.getParameters();
                } catch (RuntimeException e) {
                    Log.d(TAG, e.getMessage());
                    return;
                }
            }

            if (state) {
                params.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                mPreviewTexture = new SurfaceTexture(0);
                try {
                    mCamera.setPreviewTexture(mPreviewTexture);
                } catch (IOException e) {
                    //dont care
                    e.printStackTrace();
                }
                mCamera.startPreview();
                Log.d(TAG, "setFlash() : flash on");
            } else {
                params.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(params);
                mCamera.stopPreview();
                Log.d(TAG, "setFlash() : flash off");
            }
        } else {
            //use camera2 api for Marshmallow and UP
            if(! ctx.getApplicationContext().getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) throw new Exception("No flash available");

            CameraManager mCamManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually front camera is at 0 position.
            try {
                cameraId = mCamManager.getCameraIdList()[0];
                mCamManager.setTorchMode(cameraId, state);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        isFlashOn = state;
    }
}
