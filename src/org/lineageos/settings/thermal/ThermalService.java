// Copyright (C) 2020-2025 The LineageOS Project
// SPDX-License-Identifier: Apache-2.0

package org.lineageos.settings.thermal;

import org.lineageos.settings.Constants;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.List;

public class ThermalService extends Service {

    private static final String TAG = "ThermalService";
    private static final boolean DEBUG = false;

    private String mPreviousApp;
    private ThermalUtils mThermalUtils;
    private SharedPreferences mSharedPrefs;
    private IActivityTaskManager mActivityTaskManager;
    private ActivityManager mActivityManager;

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mPreviousApp = "";
                mThermalUtils.setDefaultThermalProfile();
                unregisterStackListener();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                registerStackListener();
            }
        }
    };

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            updateThermalProfile();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mThermalUtils = new ThermalUtils(this);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        registerStackListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThermalUtils.setDefaultThermalProfile();
        unregisterReceiver(mScreenStateReceiver);
        unregisterStackListener();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerStackListener() {
        try {
            if (mActivityTaskManager == null) {
                mActivityTaskManager = ActivityTaskManager.getService();
            }
            mActivityTaskManager.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register task stack listener", e);
        }
    }

    private void unregisterStackListener() {
        try {
            if (mActivityTaskManager != null) {
                mActivityTaskManager.unregisterTaskStackListener(mTaskListener);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to unregister task stack listener", e);
        }
    }

    private String getTopApp() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                if (topActivity != null) {
                    return topActivity.getPackageName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting top app", e);
        }
        return null;
    }

    private void updateThermalProfile() {
        try {
            String foregroundApp = getTopApp();

            if (TextUtils.isEmpty(foregroundApp)) {
                return;
            }

            if (!foregroundApp.equals(mPreviousApp)) {
                boolean isAutoSelectionMode = mSharedPrefs.getBoolean(Constants.KEY_THERMAL_AUTO_SELECTION, false);

                if (isAutoSelectionMode) {
                    mThermalUtils.setAutoThermalProfile(foregroundApp);
                } else {
                    mThermalUtils.setThermalProfile(foregroundApp);
                }
                mPreviousApp = foregroundApp;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating thermal profile", e);
        }
    }
}