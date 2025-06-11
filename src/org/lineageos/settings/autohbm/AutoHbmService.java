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

    private Handler mHandler;
    private SharedPreferences mSharedPrefs;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private boolean mIsFeatureActive = false;
    private boolean mThresholdConditionMet = false;

    private final Runnable mEnableFeatureRunnable = () -> {
        if (!mIsFeatureActive) {
            mIsFeatureActive = true;
            FileUtils.writeValue(Constants.getHbmNode(this), "1");
        }
    };

    private final Runnable mDisableFeatureRunnable = () -> {
        if (mIsFeatureActive) {
            mIsFeatureActive = false;
            FileUtils.writeValue(Constants.getHbmNode(this), "0");
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            int threshold = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_THRESHOLD, Constants.DEFAULT_AUTO_HBM_THRESHOLD);
            int timeToEnable = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_ENABLE_TIME, 0) * 1000;
            int timeToDisable = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_DISABLE_TIME, 1) * 1000;
 
            if (lux > threshold) {
                if (!mThresholdConditionMet) {
                    mThresholdConditionMet = true;
                    mHandler.removeCallbacks(mDisableFeatureRunnable);
                    mHandler.postDelayed(mEnableFeatureRunnable, timeToEnable);
                }
            } else {
                if (mThresholdConditionMet) {
                    mThresholdConditionMet = false;
                    mHandler.removeCallbacks(mEnableFeatureRunnable);
                    mHandler.postDelayed(mDisableFeatureRunnable, timeToDisable);
                }
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
        mHandler = new Handler(Looper.getMainLooper());
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
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
        FileUtils.writeValue(Constants.getHbmNode(this), "0");
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
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mEnableFeatureRunnable);
        mHandler.post(mDisableFeatureRunnable);
    }
}