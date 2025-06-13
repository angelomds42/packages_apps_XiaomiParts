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
    private UsageProgressBarPreference mCurrentBrightnessPreference;

    private int mCurrentBrightness;
    private BrightnessObserver mBrightnessObserver;
    private SharedPreferences mSharedPrefs;

    public static boolean isDcDimmingSupported(Context context) {
        String node = Constants.getDcDimmingNode(context);
        return FileUtils.fileExists(node);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_dc_dim, rootKey);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_AUTO_DC_DIMMING);
        mMainSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_AUTO_DC_DIMMING, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mThresholdPreference = findPreference(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD);
        mThresholdPreference.setOnPreferenceChangeListener(this);

        mCurrentBrightnessPreference = findPreference(Constants.KEY_CURRENT_BRIGHTNESS_LEVEL);

        mBrightnessObserver = new BrightnessObserver(new Handler(Looper.getMainLooper()));

        togglePreferencesVisibility(mMainSwitch.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMainSwitch.isChecked()) {
            mBrightnessObserver.startObserving();
        }
        updateBrightnessBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBrightnessObserver.stopObserving();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_AUTO_DC_DIMMING, isChecked).apply();

        Intent intent = new Intent(getContext(), AutoDcDimmingService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
            mBrightnessObserver.startObserving();
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
            mBrightnessObserver.stopObserving();
        }

        togglePreferencesVisibility(isChecked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThresholdPreference) {
            mSharedPrefs.edit().putInt(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD, (Integer) newValue).apply();
            updateBrightnessBar();
            return true;
        }
        return false;
    }

    private int getBrightnessProgressPercentage(int currentBrightness, int threshold) {
        if (threshold <= 0) return 0;
        if (currentBrightness > threshold) return 0;
        if (currentBrightness <= 0) return 100;
        return (int) (((float) (threshold - currentBrightness) / threshold) * 100);
    }

    private void updateBrightnessBar() {
        if (mCurrentBrightnessPreference == null) return;

        int threshold = mSharedPrefs.getInt(Constants.KEY_AUTO_DC_DIMMING_THRESHOLD,
                Constants.getDcDimmingThresholdDefault(getContext()));
        
        mCurrentBrightnessPreference.setSummary(getString(R.string.auto_dc_dim_brightness_summary_format, mCurrentBrightness, threshold));
        mCurrentBrightnessPreference.setPercent(getBrightnessProgressPercentage(mCurrentBrightness, threshold), 100);
    }

    private void togglePreferencesVisibility(boolean show) {
        mCurrentBrightnessPreference.setVisible(show);
        mThresholdPreference.setVisible(show);
    }

    private class BrightnessObserver extends ContentObserver {
        private final ContentResolver mResolver;

        public BrightnessObserver(Handler handler) {
            super(handler);
            mResolver = getContext().getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateBrightness();
        }

        public void startObserving() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.SCREEN_BRIGHTNESS), false, this);
            updateBrightness();
        }

        public void stopObserving() {
            mResolver.unregisterContentObserver(this);
        }

        private void updateBrightness() {
            try {
                mCurrentBrightness = Settings.System.getInt(mResolver, Settings.System.SCREEN_BRIGHTNESS);
                updateBrightnessBar();
            } catch (Settings.SettingNotFoundException e) {
                // Ignore
            }
        }
    }
}