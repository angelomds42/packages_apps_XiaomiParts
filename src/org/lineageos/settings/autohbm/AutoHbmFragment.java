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
    private SharedPreferences mSharedPrefs;
    private int mCurrentLux;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.auto_hbm, rootKey);
        setHasOptionsMenu(true);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_AUTO_HBM);
        mMainSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mActivationThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD);
        mActivationThresholdPreference.setOnPreferenceChangeListener(this);

        mDeactivationThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD);
        mDeactivationThresholdPreference.setOnPreferenceChangeListener(this);

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
        updateLuxBar();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, isChecked).apply();

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
        if (preference == mActivationThresholdPreference) {
            int activation = (Integer) newValue;
            int deactivation = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD,
                    Constants.getHbmDeactivationThresholdDefault(getContext()));
            if (activation <= deactivation) {
                Toast.makeText(getContext(), R.string.auto_hbm_activation_lt_deactivation_error, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
            mSharedPrefs.edit().putInt(Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD, activation).apply();
            updateLuxBar();
            return true;
        } else if (preference == mDeactivationThresholdPreference) {
            int deactivation = (Integer) newValue;
            int activation = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD,
                    Constants.getHbmActivationThresholdDefault(getContext()));
            if (deactivation >= activation) {
                Toast.makeText(getContext(), R.string.auto_hbm_deactivation_gt_activation_error, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
            mSharedPrefs.edit().putInt(Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD, deactivation).apply();
            updateLuxBar();
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            mCurrentLux = (int) event.values[0];
            updateLuxBar();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateLuxBar() {
        if (mCurrentLuxLevelPreference == null)
            return;

        int activation = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_ACTIVATION_THRESHOLD,
                Constants.getHbmActivationThresholdDefault(getContext()));
        int deactivation = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_DEACTIVATION_THRESHOLD,
                Constants.getHbmDeactivationThresholdDefault(getContext()));

        mCurrentLuxLevelPreference
                .setSummary(getString(R.string.auto_hbm_lux_summary_format, mCurrentLux, deactivation, activation));

        int range = activation - deactivation;
        int percentage = 0;
        if (range > 0) {
            int progress = mCurrentLux - deactivation;
            percentage = (int) (((float) progress / range) * 100);
        }

        if (percentage < 0)
            percentage = 0;
        if (percentage > 100)
            percentage = 100;

        mCurrentLuxLevelPreference.setPercent(percentage, 100);
    }

    private void togglePreferencesVisibility(boolean show) {
        if (mCurrentLuxLevelPreference != null)
            mCurrentLuxLevelPreference.setVisible(show);
        if (mActivationThresholdPreference != null)
            mActivationThresholdPreference.setVisible(show);
        if (mDeactivationThresholdPreference != null)
            mDeactivationThresholdPreference.setVisible(show);
    }

    public static boolean isHbmSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_autoHbmSupported);
    }
}