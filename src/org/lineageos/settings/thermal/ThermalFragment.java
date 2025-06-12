/*
 * Copyright (C) 2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.widget.CompoundButton;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.MainSwitchPreference;
import org.lineageos.settings.Constants;
import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThermalFragment extends PreferenceFragmentCompat
        implements CompoundButton.OnCheckedChangeListener {

    private static final String KEY_PER_APP_CATEGORY = "per_app_profile_category";

    private SwitchPreferenceCompat mAutoSelectionSwitch;
    private MainSwitchPreference mMainSwitch;
    private PreferenceCategory mPerAppCategory;
    private SharedPreferences mSharedPrefs;
    private String[] mProfileValues;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.thermal, rootKey);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mProfileValues = getResources().getStringArray(R.array.thermal_profile_values);

        mMainSwitch = findPreference(Constants.KEY_THERMAL_MAIN_SWITCH);
        mMainSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_THERMAL_MAIN_SWITCH, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mPerAppCategory = findPreference(KEY_PER_APP_CATEGORY);

        mAutoSelectionSwitch = findPreference("thermal_auto_selection_switch");
        mAutoSelectionSwitch.setChecked(mSharedPrefs.getBoolean("thermal_auto_selection_switch", false));
        mAutoSelectionSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isEnabled = (Boolean) newValue;
            mSharedPrefs.edit().putBoolean("thermal_auto_selection_switch", isEnabled).apply();
            mPerAppCategory.setVisible(!isEnabled);
            return true;
        });

        boolean isAutoSelectionEnabled = mSharedPrefs.getBoolean("thermal_auto_selection_switch", false);
        mPerAppCategory.setVisible(!isAutoSelectionEnabled);

        loadApps();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_THERMAL_MAIN_SWITCH, isChecked).apply();

        Intent intent = new Intent(getContext(), ThermalService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
        }
    }

    private Drawable getIconForProfile(String value) {
        if (mProfileValues == null || mProfileValues.length < 7) {
            return ContextCompat.getDrawable(getContext(), R.drawable.ic_thermal_default);
        }

        int resId;
        if (value.equals(mProfileValues[1])) { // Benchmark
            resId = R.drawable.ic_thermal_benchmark;
        } else if (value.equals(mProfileValues[2])) { // Browser
            resId = R.drawable.ic_thermal_browser;
        } else if (value.equals(mProfileValues[3])) { // Camera
            resId = R.drawable.ic_thermal_camera;
        } else if (value.equals(mProfileValues[4])) { // Dialer
            resId = R.drawable.ic_thermal_dialer;
        } else if (value.equals(mProfileValues[5])) { // Gaming
            resId = R.drawable.ic_thermal_gaming;
        } else if (value.equals(mProfileValues[6])) { // Streaming
            resId = R.drawable.ic_thermal_streaming;
        } else {
            resId = R.drawable.ic_thermal_default;
        }

        return ContextCompat.getDrawable(getContext(), resId);
    }

    private void updatePreference(AppProfilePreference pref, String value) {
        int index = pref.findIndexOfValue(value);
        if (index != -1) {
            pref.setSummary(pref.getEntries()[index]);
            pref.setProfileIcon(getIconForProfile(value));
        }
    }

    private void loadApps() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Background work
            Context context = getContext();
            if (context == null) {
                return;
            }

            PackageManager pm = context.getPackageManager();
            List<AppProfilePreference> prefs = new ArrayList<>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

            CharSequence[] entries = getResources().getTextArray(R.array.thermal_profile_entries);
            CharSequence[] entryValues = mProfileValues;

            for (ResolveInfo info : appList) {
                String packageName = info.activityInfo.packageName;
                CharSequence appName = info.loadLabel(pm);

                AppProfilePreference appPref = new AppProfilePreference(context);
                appPref.setKey(Constants.THERMAL_PREF_KEY_PREFIX + packageName);
                appPref.setTitle(appName);
                appPref.setIcon(info.loadIcon(pm));
                appPref.setEntries(entries);
                appPref.setEntryValues(entryValues);

                String currentValue = mSharedPrefs.getString(appPref.getKey(), Constants.THERMAL_DEFAULT_PROFILE_VALUE);
                appPref.setValue(currentValue);

                appPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String value = (String) newValue;
                    updatePreference((AppProfilePreference) preference, value);
                    mSharedPrefs.edit().putString(preference.getKey(), value).apply();
                    return true;
                });

                prefs.add(appPref);
            }

            mHandler.post(() -> {
                if (isAdded()) {
                    mPerAppCategory.removeAll();
                    for (Preference pref : prefs) {
                        updatePreference((AppProfilePreference) pref, ((AppProfilePreference) pref).getValue());
                        mPerAppCategory.addPreference(pref);
                    }
                }
            });
        });
    }
}