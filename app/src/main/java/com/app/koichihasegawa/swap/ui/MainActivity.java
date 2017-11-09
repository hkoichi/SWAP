package com.app.koichihasegawa.swap.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.app.koichihasegawa.swap.R;
import com.app.koichihasegawa.swap.backgroundservice.WalkCheckService;
import com.app.koichihasegawa.swap.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    public static ImageView imageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);
        imageView = binding.imageView;

        Intent i = getIntent();
        boolean isStop = i.getBooleanExtra("isStop", false);
        if (isStop) {
            stopService(new Intent(MainActivity.this, WalkCheckService.class));
            imageView.setImageBitmap(WalkCheckService.bmp);
        }
    }

    public void onClickStart(View v) {
        Intent intent = new Intent(MainActivity.this, WalkCheckService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);
        v.setVisibility(View.GONE);
        v.setClickable(false);
        binding.StopService.setVisibility(View.VISIBLE);
        binding.StopService.setClickable(true);
        binding.explainText.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    public void onClickStop(View v) {
        stopService(new Intent(MainActivity.this, WalkCheckService.class));
        v.setVisibility(View.GONE);
        v.setClickable(false);
        binding.StartService.setVisibility(View.VISIBLE);
        binding.StartService.setClickable(true);
        binding.explainText.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(MainActivity.this, WalkCheckService.class));
        super.onDestroy();
    }
}
