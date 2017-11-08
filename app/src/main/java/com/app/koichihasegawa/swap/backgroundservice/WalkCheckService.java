package com.app.koichihasegawa.swap.backgroundservice;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import java.io.IOException;
import java.util.Locale;

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
    private float steps = 0;

    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;
    private Sensor mStepConnterSensor;


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
                    mFaceDetector = Utils.setupFaceDetector(getApplicationContext());
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
                        // bitmap からmatの作成
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
            if (detectNum > 15) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("isStop", true);
                this.startActivity(intent);
            }
            MainActivity.textView.setText(Integer.toString(detectNum));
            Imgproc.rectangle(oldMat,
                    new Point(facesArray[0].tl().x * SCALE, facesArray[0].tl().y * SCALE),
                    new Point(facesArray[0].br().x * SCALE, facesArray[0].br().y * SCALE),
                    new Scalar(1, 1, 0, 1), 3);
        }
        return oldMat;
    }


    @Override
    public void onCreate() {
        //センサーマネージャを取得
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //センサマネージャから TYPE_STEP_DETECTOR についての情報を取得する
        mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        //センサマネージャから TYPE_STEP_COUNTER についての情報を取得する
        mStepConnterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        setStepCounterListener();

        super.onCreate();
    }

    private final SensorEventListener mStepCountListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            steps = sensorEvent.values[0];
            MainActivity.textViewStep.setText(String.format(Locale.US, "%f", steps));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void setStepCounterListener() {
        if (mStepConnterSensor != null) {
            //ここでセンサーリスナーを登録する
            mSensorManager.registerListener(mStepCountListener, mStepConnterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
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
