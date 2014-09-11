package org.skylight1.face;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class Skylight1FaceActivity extends Activity {

    public static final String PREFS = "org.skylight1.skylight1face.prefs";
    public static final String PREFS_DIMMED_KEY = "dimmed";

    private TextView textView;
    private Handler handler;
    private View rootView;
    private View layoutView;
    private IntentFilter timeFilter;
    private IntentFilter timeTickFilter;
    private IntentFilter timeZoneFilter;
    private BroadcastReceiver timeUpdateReceiver;
    int hour = 0;
    int minutes = 0;
    private int ANIMATION_INTERVAL = 950;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skylight1_face);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                textView = (TextView) stub.findViewById(R.id.text);
                rootView = stub.getRootView();
                layoutView = (View) stub.findViewById(R.id.layout);
                setBright();
                startRepeatingTask();
            }
        });

        handler = new Handler(Looper.getMainLooper());

        timeTickFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeFilter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        timeZoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);

        timeUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                    updateTime();
                    if (!isDimmed()) {
                        restartRepeatingTask();
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.registerReceiver(timeUpdateReceiver, timeTickFilter);
        this.registerReceiver(timeUpdateReceiver, timeFilter);
        this.registerReceiver(timeUpdateReceiver, timeZoneFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rootView != null) {
            setBright();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rootView != null) {
            setDimmed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(timeUpdateReceiver);
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = 0;
        String time = String.format("%2d:%02d", hour, minutes);
        if (!isDimmed()) {
            seconds = calendar.get(Calendar.SECOND);
            time = time+":"+String.format("%02d",seconds);
        }
        textView.setText(time);
    }

    private void setDimmed() {
        this.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREFS_DIMMED_KEY, true).commit();
        stopRepeatingTask();
        updateTime();
        layoutView.setBackground(getResources().getDrawable(android.R.drawable.screen_background_dark_transparent));
        textView.setTextColor(Color.GRAY);
    }

    private void setBright() {
        this.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREFS_DIMMED_KEY, false).commit();
        updateTime();
        startRepeatingTask();
        layoutView.setBackground(getResources().getDrawable(R.drawable.skylight1logo));
        textView.setTextColor(Color.BLACK);
    }

    private boolean isDimmed() {
        return this.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREFS_DIMMED_KEY, false);
    }

    private void startRepeatingTask() {
        handler.post(secondsAnimation);
    }

    private void stopRepeatingTask() {
        handler.removeCallbacksAndMessages(null);
    }

    private void restartRepeatingTask() {
        stopRepeatingTask();
        startRepeatingTask();
    }

    private  void updateSecondsUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateTime();
                } catch (Exception e) {
                    Log.e("skylight1face", "Error:" + e.toString());
                }
            }
        });
    }
    private Runnable secondsAnimation = new  Runnable() {
        @Override
        public void run() {
            if (!Thread.currentThread().isInterrupted()) {
                try {
                    updateSecondsUI();
                    handler.postDelayed(secondsAnimation, ANIMATION_INTERVAL);
                } catch (Exception e) {
                    Log.e("skylight1face", "Error:" + e.toString());
                }
            }

        }
    };
}
