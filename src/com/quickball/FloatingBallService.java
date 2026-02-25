package com.quickball;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class FloatingBallService extends Service {

    private WindowManager windowManager;
    private View ballView;
    private View menuView;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams menuParams;
    private boolean menuVisible = false;

    private static final String CHANNEL_ID = "quickball_channel";
    private static final int NOTIFICATION_ID = 1;

    private boolean isHidden = false;
    private boolean isOnRightSide = true;
    private int screenWidth;
    private int ballSize;
    private Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private static final long AUTO_HIDE_DELAY = 3000;

    private List<ShortcutConfig.Item> shortcuts;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("reload", false)) {
            shortcuts = ShortcutConfig.load(this);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        shortcuts = ShortcutConfig.load(this);
        createNotificationChannel();

        Intent configIntent = new Intent(this, ConfigActivity.class);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent configPi = PendingIntent.getActivity(this, 0, configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("QuickBall 运行中")
                .setContentText("点击配置快捷方式")
                .setContentIntent(configPi)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .addAction(new Notification.Action.Builder(
                    null, "配置快捷方式", configPi).build())
                .build();
        }
        startForeground(NOTIFICATION_ID, notification);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;

        createBallView();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "QuickBall Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持悬浮球运行");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void createBallView() {
        ballSize = dpToPx(48);

        FrameLayout container = new FrameLayout(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xCC444444);
        container.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_scan);
        icon.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        container.addView(icon, new FrameLayout.LayoutParams(ballSize, ballSize));

        ballView = container;

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        ballParams = new WindowManager.LayoutParams(
            ballSize, ballSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        ballParams.gravity = Gravity.TOP | Gravity.LEFT;
        ballParams.x = screenWidth - ballSize;
        ballParams.y = dpToPx(300);

        windowManager.addView(ballView, ballParams);
        isOnRightSide = true;

        setupBallTouch();
        scheduleAutoHide();
    }

    private void setupBallTouch() {
        ballView.setOnTouchListener(new View.OnTouchListener() {
            private int initX, initY;
            private float touchX, touchY;
            private boolean isDragging = false;
            private long downTime;
            private boolean longPressHandled = false;
            private boolean wasHiddenOnDown = false;
            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isDragging) {
                        longPressHandled = true;
                        Intent configIntent = new Intent(FloatingBallService.this, ConfigActivity.class);
                        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(configIntent);
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelAutoHide();
                        longPressHandled = false;
                        isDragging = false;
                        downTime = System.currentTimeMillis();
                        wasHiddenOnDown = isHidden;
                        autoHideHandler.postDelayed(longPressRunnable, 800);
                        if (isHidden) {
                            slideIn();
                        }
                        initX = ballParams.x;
                        initY = ballParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - touchX;
                        float dy = event.getRawY() - touchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            if (!isDragging) {
                                isDragging = true;
                                autoHideHandler.removeCallbacks(longPressRunnable);
                                if (wasHiddenOnDown) {
                                    ballView.animate().cancel();
                                    ballView.setTranslationX(0f);
                                    ballView.setAlpha(1.0f);
                                    isHidden = false;
                                }
                            }
                        }
                        if (isDragging) {
                            ballParams.x = initX + (int) dx;
                            ballParams.y = initY + (int) dy;
                            windowManager.updateViewLayout(ballView, ballParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        autoHideHandler.removeCallbacks(longPressRunnable);
                        if (longPressHandled) return true;
                        if (wasHiddenOnDown && !isDragging) {
                            showMenu();
                            scheduleAutoHide();
                            return true;
                        }
                        if (!isDragging && (System.currentTimeMillis() - downTime < 300)) {
                            toggleMenu();
                        } else if (isDragging) {
                            snapToEdge();
                        }
                        if (!menuVisible) {
                            scheduleAutoHide();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void snapToEdge() {
        int centerX = ballParams.x + ballSize / 2;
        int targetX;
        if (centerX < screenWidth / 2) {
            targetX = 0;
            isOnRightSide = false;
        } else {
            targetX = screenWidth - ballSize;
            isOnRightSide = true;
        }

        ValueAnimator anim = ValueAnimator.ofInt(ballParams.x, targetX);
        anim.setDuration(200);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ballParams.x = (int) animation.getAnimatedValue();
                try {
                    windowManager.updateViewLayout(ballView, ballParams);
                } catch (Exception e) {}
            }
        });
        anim.start();
    }

    /** Slide ball half off screen using translationX (bypasses WM clipping) */
    private void slideOut() {
        if (isHidden) return;
        float targetTx = isOnRightSide ? ballSize / 2f : -ballSize / 2f;
        ballView.animate()
            .translationX(targetTx)
            .alpha(0.3f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        isHidden = true;
    }

    /** Slide back and show menu in one action */
    private void slideInAndShowMenu() {
        ballView.animate()
            .translationX(0f)
            .alpha(1.0f)
            .setDuration(200)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ballView.animate().setListener(null);
                    isHidden = false;
                    showMenu();
                    scheduleAutoHide();
                }
            })
            .start();
    }

    /** Slide back without showing menu */
    private void slideIn() {
        ballView.animate()
            .translationX(0f)
            .alpha(1.0f)
            .setDuration(200)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        isHidden = false;
        scheduleAutoHide();
    }

    private Runnable autoHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (menuVisible) {
                hideMenu();
            }
            slideOut();
        }
    };

    private void scheduleAutoHide() {
        cancelAutoHide();
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
    }

    private void cancelAutoHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    private void toggleMenu() {
        if (menuVisible) {
            hideMenu();
            scheduleAutoHide();
        } else {
            showMenu();
            scheduleAutoHide();
        }
    }

    private void showMenu() {
        if (menuVisible) return;

        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setCornerRadius(dpToPx(16));
        menuBg.setColor(0xEE333333);
        menuLayout.setBackground(menuBg);

        for (int i = 0; i < shortcuts.size(); i++) {
            final ShortcutConfig.Item sc = shortcuts.get(i);

            TextView item = new TextView(this);
            item.setText(sc.label);
            item.setTextColor(Color.WHITE);
            item.setTextSize(14);
            item.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10));

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setCornerRadius(dpToPx(20));
            try {
                itemBg.setColor(Color.parseColor(sc.color));
            } catch (Exception e) {
                itemBg.setColor(0x66FFFFFF);
            }
            item.setBackground(itemBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dpToPx(4), 0, dpToPx(4));
            item.setLayoutParams(lp);
            item.setGravity(Gravity.CENTER);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideMenu();
                    if ("system".equals(sc.type) && "screenshot".equals(sc.systemKey)) {
                        slideOut();
                        autoHideHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ShortcutConfig.executeShortcut(FloatingBallService.this, sc);
                            }
                        }, 500);
                    } else {
                        ShortcutConfig.executeShortcut(FloatingBallService.this, sc);
                        scheduleAutoHide();
                    }
                }
            });

            menuLayout.addView(item);
        }

        menuView = menuLayout;

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        menuParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        if (isOnRightSide) {
            menuParams.gravity = Gravity.TOP | Gravity.RIGHT;
            menuParams.x = (screenWidth - ballParams.x) + dpToPx(4);
        } else {
            menuParams.gravity = Gravity.TOP | Gravity.LEFT;
            menuParams.x = ballParams.x + ballSize + dpToPx(4);
        }
        menuParams.y = ballParams.y - dpToPx(40);

        menuView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    hideMenu();
                    slideOut();
                    return true;
                }
                return false;
            }
        });

        windowManager.addView(menuView, menuParams);
        menuVisible = true;
    }

    private void hideMenu() {
        if (menuView != null && menuVisible) {
            windowManager.removeView(menuView);
            menuView = null;
            menuVisible = false;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        scheduleRestart();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        cancelAutoHide();
        if (ballView != null) {
            try { windowManager.removeView(ballView); } catch (Exception e) {}
        }
        hideMenu();
        scheduleRestart();
        super.onDestroy();
    }

    private void scheduleRestart() {
        Intent restartIntent = new Intent(this, FloatingBallService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
            this, 1, restartIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000, pendingIntent);
    }
}
