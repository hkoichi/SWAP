package com.app.koichihasegawa.swap.backgroundservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.Log;

import com.app.koichihasegawa.swap.lib.Utils;
import com.app.koichihasegawa.swap.ui.MainActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;


/**
 * Created by koichihasegawa on 2017/10/31.
 */

public class WalkCheckService extends Service {
    public static Camera camera = null;
    private static final String TAG = "OCVSample::Activity";
    private SurfaceTexture mSurfaceTexture = new SurfaceTexture(10);

    private Camera.Parameters parameters;
    private int cameraWidth = 0;
    private int cameraHeight = 0;

    // スレッドを制御する変数
    private boolean runnning = true;

    // opencv が読み込まれた後に呼ばれるコールバック
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(WalkCheckService.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("", "OpenCV loaded successfully");
                    startImageProcessingThread();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // 画像処理スレッドの呼び出し
    public void startImageProcessingThread() {
        Thread thread = new Thread() {
            public void run() {
                    camera = Camera.open(1);
                    camera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            if (!runnning) {
                                return;
                            }
                            // frame からbitmapの作成
                            Bitmap bmp = Utils.makeBitmap(data, cameraWidth, cameraHeight, parameters);
                            // bitma からmatの作成
                            Mat m = new Mat();
                            bitmapToMat(bmp, m);
                            // matからbitmapの作成
                            matToBitmap(m, bmp);
                            MainActivity.imageView.setImageBitmap(bmp);
                        }
                    });
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        // opencv のロード
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onDestroy() {
        runnning = false;
        mSurfaceTexture.detachFromGLContext();
        mSurfaceTexture.release();
        camera.stopPreview();
        camera.lock();
        camera = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
