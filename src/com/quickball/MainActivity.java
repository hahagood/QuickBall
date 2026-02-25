package com.quickball;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShortcutConfig.initIfNeeded(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("QuickBall 悬浮球");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        TextView desc = new TextView(this);
        desc.setText("\n授权悬浮窗权限后，屏幕边缘将出现快捷球。\n\n点击半隐藏的球 → 直接弹出菜单\n拖动可改变位置\n长按悬浮球 → 配置快捷方式\n3秒无操作自动隐藏到边缘\n");
        desc.setTextSize(16);
        desc.setGravity(Gravity.CENTER);

        Button btn = new Button(this);
        btn.setText("启动悬浮球");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryStartService();
            }
        });

        Button configBtn = new Button(this);
        configBtn.setText("配置快捷方式");
        configBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ConfigActivity.class));
            }
        });

        root.addView(title);
        root.addView(desc);
        root.addView(btn);
        root.addView(configBtn);
        setContentView(root);

        tryStartService();
    }

    private void tryStartService() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
        } else {
            requestBatteryOptimization();
            startBallService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                requestBatteryOptimization();
                startBallService();
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能使用", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                // not supported on this device
            }
        }
    }

    private void startBallService() {
        Intent svc = new Intent(this, FloatingBallService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        Toast.makeText(this, "悬浮球已启动", Toast.LENGTH_SHORT).show();
        finish();
    }
}
