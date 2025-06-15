package org.lineageos.settings.autodcdim;

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

    private SharedPreferences mSharedPrefs;
    private BrightnessObserver mBrightnessObserver;

    private Handler mHandler;
    private final Runnable mEnableDCDimmingRunnable = () -> FileUtils.writeValue(getNodePath(), "1");
    private final Runnable mDisableDCDimmingRunnable = () -> FileUtils.writeValue(getNodePath(), "0");

    private static final int DC_DIMMING_DELAY_MS = 200;

    private String getNodePath() {
        return Constants.getDcDimmingNode(this);
    }

    private void handleBrightnessChange() {
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            int threshold = mSharedPrefs.getInt(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD,
                    Constants.getDefaultAutoDcDimmingThreshold(this));

            if (brightness < threshold) {
                mHandler.removeCallbacks(mDisableDCDimmingRunnable);
                mHandler.postDelayed(mEnableDCDimmingRunnable, DC_DIMMING_DELAY_MS);
            } else {
                mHandler.removeCallbacks(mEnableDCDimmingRunnable);
                mHandler.postDelayed(mDisableDCDimmingRunnable, DC_DIMMING_DELAY_MS);
            }
        } catch (Settings.SettingNotFoundException e) {
            mHandler.removeCallbacks(mEnableDCDimmingRunnable);
            mHandler.postDelayed(mDisableDCDimmingRunnable, DC_DIMMING_DELAY_MS);
        }
    }

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
        }
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mBrightnessObserver.startObserving();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mBrightnessObserver.stopObserving();
                mHandler.removeCallbacksAndMessages(null);
                FileUtils.writeValue(getNodePath(), "0");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler(Looper.getMainLooper());
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
        mHandler.removeCallbacksAndMessages(null);
        FileUtils.writeValue(getNodePath(), "0");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}