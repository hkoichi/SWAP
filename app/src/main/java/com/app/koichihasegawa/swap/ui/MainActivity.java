package com.app.koichihasegawa.swap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.koichihasegawa.swap.R;
import com.app.koichihasegawa.swap.backgroundservice.WalkCheckService;

import static com.app.koichihasegawa.swap.backgroundservice.WalkCheckService.camera;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "Recorder";
    public static SurfaceView mSurfaceView;
    public static SurfaceHolder mSurfaceHolder;
    public static ImageView imageView;
    public static TextView textView;

    private RecordMode recordMode = RecordMode.WAITING;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        final Button btnStart = (Button) findViewById(R.id.StartService);
        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (recordMode) {
                    case RECORDIND:
                        stopService(new Intent(MainActivity.this, WalkCheckService.class));
                        btnStart.setText("start");
                        recordMode = RecordMode.WAITING;
                        Toast.makeText(getApplicationContext(), "stop checking", Toast.LENGTH_SHORT).show();
                        break;
                    case WAITING:
                        Intent intent = new Intent(MainActivity.this, WalkCheckService.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startService(intent);
                        btnStart.setText("stop");
                        recordMode = RecordMode.RECORDIND;
                        Toast.makeText(getApplicationContext(), "start checking", Toast.LENGTH_SHORT).show();

                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        camera.release();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    private enum RecordMode {
        RECORDIND,
        WAITING
    }
}
