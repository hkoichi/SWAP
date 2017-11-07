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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
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

    final double SCALE = 1;
    final double MINSCALE = 7;
    final double MAXSCALE = 0.5;

    private Camera.Parameters parameters;
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private Size mMinFaceSize = new Size(0, 0);
    private Size mMaxFaceSize = new Size(0, 0);

    private int detectNum = 0;

    // スレッドを制御する変数
    private boolean runnning = true;
    // cascade classifierを用いた顔認証をするインスタンス
    private CascadeClassifier mFaceDetector;

    // opencv が読み込まれた後に呼ばれるコールバック
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(WalkCheckService.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("", "OpenCV loaded successfully");
                    mFaceDetector = setupFaceDetector();
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
                        if (cameraWidth == 0) {
                            parameters = camera.getParameters();
                            cameraWidth = parameters.getPreviewSize().width;
                            cameraHeight = parameters.getPreviewSize().height;
                        }
                        // frame からbitmapの作成
                        Bitmap bmp = Utils.makeBitmap(data, cameraWidth, cameraHeight, parameters);
                        // bitma からmatの作成
                        Mat oldMat = new Mat();
                        bitmapToMat(bmp, oldMat);
                        // image processing
                        Mat newMat = imageProcessing(oldMat);
                        // matからbitmapの作成
                        matToBitmap(newMat, bmp);
                        // 反映
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

    // 画像処理
    private Mat imageProcessing(Mat oldMat) {
        if (mMinFaceSize.width == 0) {
            mMinFaceSize = new Size(cameraHeight / MINSCALE, cameraWidth / MINSCALE);
            mMaxFaceSize = new Size(cameraHeight / MAXSCALE, cameraWidth / MAXSCALE);
        }
        Mat smallImg = new Mat(new Size(cameraHeight / SCALE, cameraWidth / SCALE), CvType.CV_8UC1);

        Mat gray = new Mat();
        Imgproc.cvtColor(oldMat, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.resize(gray, smallImg, smallImg.size(), 0, 0, Imgproc.INTER_LINEAR);
        Imgproc.equalizeHist(smallImg, smallImg);

        MatOfRect faces = new MatOfRect();
        mFaceDetector.detectMultiScale(smallImg, faces,
                1.1,
                2,
                2,
                mMinFaceSize,
                mMaxFaceSize);
        Rect[] facesArray = faces.toArray();
        if (facesArray.length > 0) {
            detectNum++;
            MainActivity.textView.setText(Integer.toString(detectNum));
            Imgproc.rectangle(oldMat,
                    new Point(facesArray[0].tl().x * SCALE, facesArray[0].tl().y * SCALE),
                    new Point(facesArray[0].br().x * SCALE, facesArray[0].br().y * SCALE),
                    new Scalar(1, 1, 0, 1), 3);
        }
        return oldMat;
    }

    // cascade classifierをset up
    private CascadeClassifier setupFaceDetector() {
        File cascadeFile = Utils.setUpCascadeFile(getApplicationContext());
        if (cascadeFile == null) {
            return null;
        }

        CascadeClassifier detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (detector.empty()) {
            return null;
        }
        return detector;
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
