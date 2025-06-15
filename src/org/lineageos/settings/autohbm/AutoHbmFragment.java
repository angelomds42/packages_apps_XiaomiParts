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
import android.widget.Toast;

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
    private CustomSeekBarPreference mActivationThresholdPreference;
    private CustomSeekBarPreference mDeactivationThresholdPreference;
    private UsageProgressBarPreference mCurrentLuxLevelPreference;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private int mCurrentLux;

    private static final int THRESHOLD_INTERVAL = 100;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_hbm, rootKey);
        setHasOptionsMenu(true);

        Context context = getContext();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_AUTO_HBM);
        mMainSwitch.setChecked(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mActivationThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD);
        mActivationThresholdPreference.setOnPreferenceChangeListener(this);

        mDeactivationThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD);
        mDeactivationThresholdPreference.setOnPreferenceChangeListener(this);

        mCurrentLuxLevelPreference = findPreference(Constants.KEY_CURRENT_LUX_LEVEL);

        mCurrentLuxLevelPreference.setVisible(mMainSwitch.isChecked());

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
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

        mCurrentLuxLevelPreference.setVisible(isChecked);

        Intent intent = new Intent(getContext(), AutoHbmService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int newActivationThreshold = mActivationThresholdPreference.getValue();
        int newDeactivationThreshold = mDeactivationThresholdPreference.getValue();

        if (preference == mActivationThresholdPreference) {
            newActivationThreshold = (Integer) newValue;
            if (newActivationThreshold <= newDeactivationThreshold) {
                newDeactivationThreshold = newActivationThreshold - THRESHOLD_INTERVAL;
                mDeactivationThresholdPreference.refresh(newDeactivationThreshold);
            }
        } else if (preference == mDeactivationThresholdPreference) {
            newDeactivationThreshold = (Integer) newValue;
            if (newActivationThreshold <= newDeactivationThreshold) {
                newActivationThreshold = newDeactivationThreshold + THRESHOLD_INTERVAL;
                mActivationThresholdPreference.refresh(newActivationThreshold);
            }
        }

        updateCurrentLuxLevelPreference(mCurrentLux, newActivationThreshold);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            mCurrentLux = (int) event.values[0];
            int activationThreshold = mActivationThresholdPreference.getValue();
            updateCurrentLuxLevelPreference(mCurrentLux, activationThreshold);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateCurrentLuxLevelPreference(int currentLux, int threshold) {
        if (mCurrentLuxLevelPreference != null) {
            mCurrentLuxLevelPreference.setUsageSummary(String.valueOf(currentLux));
            mCurrentLuxLevelPreference.setTotalSummary(String.valueOf(threshold));
            mCurrentLuxLevelPreference.setPercent(getLuxProgressPercentage(currentLux, threshold), 100);
        }
    }

    private int getLuxProgressPercentage(int currentLux, int threshold) {
        if (currentLux >= threshold)
            return 100;
        if (threshold <= 0)
            return 0;
        return (int) (((float) currentLux / threshold) * 100);
    }

    public static boolean isHbmSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_autoHbmSupported);
    }
}