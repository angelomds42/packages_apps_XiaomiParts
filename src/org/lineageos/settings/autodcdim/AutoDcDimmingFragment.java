package org.lineageos.settings.autodcdim;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.UsageProgressBarPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class AutoDcDimmingFragment extends PreferenceFragmentCompat
        implements CompoundButton.OnCheckedChangeListener, Preference.OnPreferenceChangeListener {

    private MainSwitchPreference mMainSwitch;
    private CustomSeekBarPreference mThresholdPreference;
    private UsageProgressBarPreference mCurrentBrightnessLevelPreference;
    private Preference mSetCurrentBrightnessButton;
    private SharedPreferences mSharedPrefs;

    private Handler mHandler;
    private BrightnessObserver mBrightnessObserver;
    private int mCurrentBrightness;

    public static boolean isDcDimmingSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_autoDcDimmingSupported);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_dc_dim, rootKey);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_AUTO_DC_DIMMING);
        mMainSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_AUTO_DC_DIMMING, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mCurrentBrightnessLevelPreference = findPreference(Constants.KEY_CURRENT_BRIGHTNESS_LEVEL);

        mThresholdPreference = findPreference(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD);
        mThresholdPreference.setOnPreferenceChangeListener(this);

        mSetCurrentBrightnessButton = findPreference("set_current_brightness_button");

        mSetCurrentBrightnessButton.setOnPreferenceClickListener(preference -> {
            try {
                int currentBrightness = Settings.System.getInt(getContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS);

                mThresholdPreference.refresh(currentBrightness);

                updateCurrentBrightnessLevelPreference(currentBrightness, currentBrightness);
            } catch (Settings.SettingNotFoundException e) {
            }
            return true;
        });

        mHandler = new Handler(Looper.getMainLooper());
        mBrightnessObserver = new BrightnessObserver(mHandler);

        mCurrentBrightnessLevelPreference.setVisible(mMainSwitch.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMainSwitch.isChecked()) {
            mBrightnessObserver.startObserving();
            updateCurrentBrightness();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBrightnessObserver.stopObserving();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_AUTO_DC_DIMMING, isChecked).apply();
        
        mCurrentBrightnessLevelPreference.setVisible(isChecked);

        Intent intent = new Intent(getContext(), AutoDcDimmingService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
            mBrightnessObserver.startObserving();
            updateCurrentBrightness();
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
            mBrightnessObserver.stopObserving();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThresholdPreference) {
            final int threshold = (Integer) newValue;
            updateCurrentBrightnessLevelPreference(mCurrentBrightness, threshold);
        }
        return true;
    }

    private void updateCurrentBrightness() {
        try {
            mCurrentBrightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            updateCurrentBrightnessLevelPreference(mCurrentBrightness, mThresholdPreference.getValue());
        } catch (Settings.SettingNotFoundException e) {
        }
    }

    private void updateCurrentBrightnessLevelPreference(int currentBrightness, int threshold) {
        if (mCurrentBrightnessLevelPreference != null) {
            mCurrentBrightnessLevelPreference.setUsageSummary(String.valueOf(currentBrightness));
            mCurrentBrightnessLevelPreference.setTotalSummary(String.valueOf(threshold));
            mCurrentBrightnessLevelPreference.setPercent(getBrightnessProgressPercentage(currentBrightness, threshold),
                    100);
        }
    }

    private int getBrightnessProgressPercentage(int currentBrightness, int threshold) {
        if (threshold <= 0)
            return 0;
        int progress = (int) (100 * ((float) currentBrightness / (float) threshold));
        return Math.max(0, Math.min(100, progress));
    }

    private class BrightnessObserver extends ContentObserver {
        BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateCurrentBrightness();
        }

        void startObserving() {
            ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS), false, this);
        }

        void stopObserving() {
            getContext().getContentResolver().unregisterContentObserver(this);
        }
    }
}