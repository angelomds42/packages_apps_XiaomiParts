package org.lineageos.settings.autodcdim;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class AutoDcDimmingService extends Service {

    private static final int DC_TRANSITION_DELAY_MS = 200;

    private Handler mHandler;
    private SharedPreferences mSharedPrefs;
    private BrightnessObserver mBrightnessObserver;

    private boolean mIsFeatureActive = false;
    private boolean mThresholdConditionMet = false;

    private String getNodePath() {
        return Constants.getDcDimmingNode(this);
    }

    private final Runnable mEnableFeatureRunnable = () -> {
        if (!mIsFeatureActive) {
            mIsFeatureActive = true;
            FileUtils.writeValue(getNodePath(), "1");
        }
    };

    private final Runnable mDisableFeatureRunnable = () -> {
        if (mIsFeatureActive) {
            mIsFeatureActive = false;
            FileUtils.writeValue(getNodePath(), "0");
        }
    };

    private class BrightnessObserver extends ContentObserver {
        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            handleBrightnessChange();
        }

        public void startObserving() {
            final ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS), false, this);
            handleBrightnessChange();
        }

        public void stopObserving() {
            final ContentResolver resolver = getContentResolver();
            resolver.unregisterContentObserver(this);
            mHandler.removeCallbacksAndMessages(null);
            if (mIsFeatureActive) {
                mIsFeatureActive = false;
                FileUtils.writeValue(getNodePath(), "0");
            }
        }
    }

    private void handleBrightnessChange() {
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            int threshold = mSharedPrefs.getInt(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD, 
                                                 Constants.getDcDimmingThresholdDefault(this));

            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.inKeyguardRestrictedInputMode()) {
                return;
            }

            if (brightness < threshold) {
                if (!mThresholdConditionMet) {
                    mThresholdConditionMet = true;
                    mHandler.removeCallbacks(mDisableFeatureRunnable);
                    mHandler.postDelayed(mEnableFeatureRunnable, DC_TRANSITION_DELAY_MS);
                }
            } else {
                if (mThresholdConditionMet) {
                    mThresholdConditionMet = false;
                    mHandler.removeCallbacks(mEnableFeatureRunnable);
                    mHandler.postDelayed(mDisableFeatureRunnable, DC_TRANSITION_DELAY_MS);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            mHandler.post(mDisableFeatureRunnable);
        }
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mBrightnessObserver.startObserving();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mBrightnessObserver.stopObserving();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mBrightnessObserver = new BrightnessObserver(mHandler);

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter, Context.RECEIVER_NOT_EXPORTED);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            mBrightnessObserver.startObserving();
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
        mBrightnessObserver.stopObserving();
        FileUtils.writeValue(getNodePath(), "0");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}