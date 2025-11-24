package org.lineageos.settings.refreshrate;

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
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

public class RefreshService extends Service {

    private static final String TAG = "RefreshService";
    private String mPreviousApp;
    private RefreshUtils mRefreshUtils;
    private IActivityTaskManager mActivityTaskManager;
    private ActivityManager mActivityManager;

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mPreviousApp = "";
                mRefreshUtils.restoreDefaultProfile();
            }
        }
    };

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            updateRefreshProfile();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mRefreshUtils = new RefreshUtils(this);
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, filter);

        registerStackListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRefreshUtils.restoreDefaultProfile();
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

    private void updateRefreshProfile() {
        try {
            String foregroundApp = getTopApp();
            if (foregroundApp != null && !foregroundApp.equals(mPreviousApp)) {
                mRefreshUtils.setRefreshProfile(foregroundApp);
                mPreviousApp = foregroundApp;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating refresh profile", e);
        }
    }
}
