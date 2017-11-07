package com.app.koichihasegawa.swap.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

import static com.app.koichihasegawa.swap.backgroundservice.WalkCheckService.camera;

/**
 * Created by koichihasegawa on 2017/11/07.
 */

public class Utils {
    // byte[] からbitmapを作成する関数
    public static Bitmap makeBitmap(byte[] data, int cameraWidth, int cameraHeight, Camera.Parameters parameters) {
        if (cameraWidth == 0) {
            parameters = camera.getParameters();
            cameraWidth = parameters.getPreviewSize().width;
            cameraHeight = parameters.getPreviewSize().height;
        }
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
}
