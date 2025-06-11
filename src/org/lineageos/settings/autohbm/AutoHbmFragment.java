/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.autohbm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.CompoundButton;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.UsageProgressBarPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.R;

public class AutoHbmFragment extends PreferenceFragmentCompat
        implements CompoundButton.OnCheckedChangeListener, SensorEventListener, Preference.OnPreferenceChangeListener {

    private MainSwitchPreference mMainSwitch;
    private CustomSeekBarPreference mThresholdPreference;
    private UsageProgressBarPreference mCurrentLuxLevelPreference;
    private CustomSeekBarPreference mEnableTimePreference;
    private CustomSeekBarPreference mDisableTimePreference;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private int mCurrentLux;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_hbm, rootKey);
        setHasOptionsMenu(true);

        Context context = getContext();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_AUTO_HBM);
        mMainSwitch.setChecked(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_THRESHOLD);
        mThresholdPreference.setOnPreferenceChangeListener(this);

        mEnableTimePreference = findPreference(Constants.KEY_AUTO_HBM_ENABLE_TIME);
        mDisableTimePreference = findPreference(Constants.KEY_AUTO_HBM_DISABLE_TIME);

        mCurrentLuxLevelPreference = findPreference(Constants.KEY_CURRENT_LUX_LEVEL);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
 
        togglePreferencesVisibility(mMainSwitch.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMainSwitch.isChecked()) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, isChecked).apply();

        Intent intent = new Intent(getContext(), AutoHbmService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
            mSensorManager.unregisterListener(this);
        }

        togglePreferencesVisibility(isChecked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThresholdPreference && mCurrentLuxLevelPreference != null) {
            int threshold = (int) newValue;
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            mCurrentLux = (int) event.values[0];
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int threshold = sharedPrefs.getInt(Constants.KEY_AUTO_HBM_THRESHOLD, Constants.DEFAULT_AUTO_HBM_THRESHOLD);
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateCurrentLuxLevelPreference(int currentLux, int threshold) {
        if (mCurrentLuxLevelPreference != null) {
            mCurrentLuxLevelPreference.setUsageSummary(String.valueOf(currentLux));
            mCurrentLuxLevelPreference.setTotalSummary(String.valueOf(threshold));
            mCurrentLuxLevelPreference.setPercent(getLuxProgressPercentage(currentLux, threshold),100);
        }
    }

    private void togglePreferencesVisibility(boolean show) {
        if (mCurrentLuxLevelPreference != null) mCurrentLuxLevelPreference.setVisible(show);
        if (mThresholdPreference != null) mThresholdPreference.setVisible(show);
        if (mEnableTimePreference != null) mEnableTimePreference.setVisible(show);
        if (mDisableTimePreference != null) mDisableTimePreference.setVisible(show);
    }

    private int getLuxProgressPercentage(int currentLux, int threshold) {
        if (currentLux >= threshold) return 100;
        if (threshold <= 0) return 0;
        return (int) (((float) currentLux / threshold) * 100);
    }

    public static boolean isHbmSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_autoHbmSupported);
    }
}