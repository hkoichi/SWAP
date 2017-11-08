package com.app.koichihasegawa.swap.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.koichihasegawa.swap.R;
import com.app.koichihasegawa.swap.backgroundservice.WalkCheckService;
import com.app.koichihasegawa.swap.databinding.ActivityMainBinding;

import static com.app.koichihasegawa.swap.backgroundservice.WalkCheckService.camera;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    public static ImageView imageView;
    public static TextView textView;
    public static TextView textViewStep;

    private ActivityMainBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);

        Intent i = getIntent();
        boolean isStop = i.getBooleanExtra("isStop", false);
        if (isStop) {
            stopService(new Intent(MainActivity.this, WalkCheckService.class));
        }

        imageView = binding.imageView;
        textView = binding.textView;
        textViewStep = binding.textViewStep;

    }

    public void onClickStart(View v) {
        Intent intent = new Intent(MainActivity.this, WalkCheckService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);
        Toast.makeText(getApplicationContext(), "start checking", Toast.LENGTH_SHORT).show();
        v.setVisibility(View.GONE);
        v.setClickable(false);
        binding.StopService.setVisibility(View.VISIBLE);
        binding.StopService.setClickable(true);
    }

    public void onClickStop(View v) {
        stopService(new Intent(MainActivity.this, WalkCheckService.class));
        Toast.makeText(getApplicationContext(), "stop checking", Toast.LENGTH_SHORT).show();
        v.setVisibility(View.GONE);
        v.setClickable(false);
        binding.StartService.setVisibility(View.VISIBLE);
        binding.StartService.setClickable(true);
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
}
