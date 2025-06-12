// Copyright (C) 2020-2025 The LineageOS Project
// SPDX-License-Identifier: Apache-2.0

package org.lineageos.settings.thermal;

import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

public class ThermalService extends Service {

    private static final String TAG = "ThermalService";

    private String mPreviousApp;
    private ThermalUtils mThermalUtils;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private IActivityTaskManager mActivityTaskManager;

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mPreviousApp = "";
                mThermalUtils.setDefaultThermalProfile();
                unregisterStackListener();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                registerStackListener();
                mHandler.postDelayed(() -> updateThermalProfile(), 500);
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
            mActivityTaskManager = ActivityTaskManager.getService();
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

    private void updateThermalProfile() {
        try {
            final RootTaskInfo focusedTask = mActivityTaskManager.getFocusedRootTaskInfo();
            if (focusedTask == null || focusedTask.topActivity == null) {
                return;
            }
            String foregroundApp = focusedTask.topActivity.getPackageName();
            if (!foregroundApp.equals(mPreviousApp)) {
                mThermalUtils.setThermalProfile(foregroundApp);
                mPreviousApp = foregroundApp;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating thermal profile", e);
        }
    }
}