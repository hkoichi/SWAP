package com.app.koichihasegawa.swap.backgroundservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Created by koichihasegawa on 2017/10/31.
 */

public class WalkCheckService extends Service {
    private Camera camera = null;
    private int NOTIFICATION_ID = 1;
    private static final String TAG = "OCVSample::Activity";
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private WindowManager windowManager;
    private SurfaceTexture mSurfaceTexture = new SurfaceTexture(10);
    Intent intializerIntent;


    public class LocalBinder extends Binder {
        WalkCheckService getService() {
            // Return this instance of this service so clients can call public methods
            return WalkCheckService.this;
        }
    }//end inner class that returns an instance of the service.

    @Override
    public IBinder onBind(Intent intent) {
        intializerIntent = intent;
        return mBinder;
    }//end onBind.


    @Override
    public void onCreate() {

        Log.i(TAG, "onCreate is called");
        // Start foreground service to avoid unexpected kill
        Thread thread = new Thread() {
            public void run() {

                camera = Camera.open(1);
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        Camera.Parameters parameters = camera.getParameters();
                        int width = parameters.getPreviewSize().width;
                        int height = parameters.getPreviewSize().height;

                        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

                        byte[] bytes = out.toByteArray();
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        // MainActivity.imageView.setImageBitmap(bitmap);
                    }
                });

                //now try to set the preview texture of the camera which is actually the  surfaceTexture that has just been created.
                try {
                    camera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException e) {
                    Log.e(TAG, "Error in setting the camera surface texture");
                }

                camera.startPreview();
            }


        };
        thread.start();

    }


    @Override
    public void onDestroy() {

        Log.i(TAG, "surfaceDestroyed method");

        camera.stopPreview();
        camera.lock();

        camera.release();
        mSurfaceTexture.detachFromGLContext();
        mSurfaceTexture.release();
        //stopService(intializerIntent);
        //windowManager.removeView(surfaceView);
    }

}
