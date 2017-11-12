package com.app.koichihasegawa.swap.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import com.app.koichihasegawa.swap.R;

import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by koichihasegawa on 2017/11/07.
 */

public class Utils {


    // byte[] からbitmapを作成する関数
    public static Bitmap makeBitmap(byte[] data, int cameraWidth, int cameraHeight, Camera.Parameters parameters) {
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), cameraWidth, cameraHeight, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, cameraWidth, cameraHeight), 50, out);

        byte[] bytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        // 回転マトリックス作成（90度回転）
        Matrix mat = new Matrix();
        mat.postRotate(-90);
        // 回転したビットマップを作成
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, cameraWidth, cameraHeight, mat, true);
        return bmp;
    }
    // cascade file を読み込む関数
    public static File setUpCascadeFile(Context mContext, Boolean isEye) {
        File cascadeDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = null;
        InputStream is = null;
        FileOutputStream os = null;
        try {
            if(isEye) {
                cascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
            } else {
                cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            }
            if (!cascadeFile.exists()) {
                is = mContext.getResources().openRawResource(R.raw.lbpcascade_frontalface);
                os = new FileOutputStream(cascadeFile);
                byte[] buffer = new byte[4096];
                int readLen = 0;
                while ((readLen = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readLen);
                }
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
        return cascadeFile;
    }

    // cascade classifierをset up
    public static CascadeClassifier setupFaceDetector(Context context, Boolean isEye) {
        File cascadeFile = Utils.setUpCascadeFile(context, isEye);
        if (cascadeFile == null) {
            return null;
        }

        CascadeClassifier detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (detector.empty()) {
            return null;
        }
        return detector;
    }
}
