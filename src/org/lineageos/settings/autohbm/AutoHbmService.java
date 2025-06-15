/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.autohbm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class AutoHbmService extends Service implements SensorEventListener {

    private SharedPreferences mSharedPrefs;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private Handler mHandler;
    private boolean mIsFeatureActive = false;

    private final Runnable mEnableHBMRunnable = () -> {
        FileUtils.writeValue(Constants.getHbmNode(this), "1");
        mIsFeatureActive = true;
    };

    private final Runnable mDisableHBMRunnable = () -> {
        FileUtils.writeValue(Constants.getHbmNode(this), "0");
        mIsFeatureActive = false;
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            int activationThreshold = mSharedPrefs.getInt(
                    Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD,
                    Constants.getDefaultAutoHbmActivationThreshold(this));
            int deactivationThreshold = mSharedPrefs.getInt(
                    Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD,
                    Constants.getDefaultAutoHbmDeactivationThreshold(this));

            int hbmDelay = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_DELAY, 700);

            if (!mIsFeatureActive && lux > activationThreshold) {
                mHandler.removeCallbacks(mDisableHBMRunnable);
                mHandler.postDelayed(mEnableHBMRunnable, hbmDelay);
            } else if (mIsFeatureActive && lux < deactivationThreshold) {
                mHandler.removeCallbacks(mEnableHBMRunnable);
                mHandler.postDelayed(mDisableHBMRunnable, hbmDelay);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                startListening();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                stopListening();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler(Looper.getMainLooper());
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter, Context.RECEIVER_NOT_EXPORTED);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            startListening();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        stopListening();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startListening() {
        if (mLightSensor != null) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopListening() {
        if (mLightSensor != null) {
            mSensorManager.unregisterListener(this);
        }
        mHandler.removeCallbacksAndMessages(null);
        if (mIsFeatureActive) {
            FileUtils.writeValue(Constants.getHbmNode(this), "0");
            mIsFeatureActive = false;
        }
    }
}