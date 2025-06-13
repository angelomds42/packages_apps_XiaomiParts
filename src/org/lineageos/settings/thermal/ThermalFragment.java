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
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThermalFragment extends PreferenceFragmentCompat
        implements CompoundButton.OnCheckedChangeListener {

    private static final String KEY_PER_APP_CATEGORY = "per_app_profile_category";

    private SwitchPreferenceCompat mAutoSelectionSwitch;
    private MainSwitchPreference mMainSwitch;
    private PreferenceCategory mPerAppCategory;
    private SharedPreferences mSharedPrefs;
    private String[] mProfileValues;

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

        new LoadAppsTask(this).execute();
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

    private static class LoadAppsTask extends AsyncTask<Void, Void, List<AppProfilePreference>> {

        private WeakReference<ThermalFragment> fragmentReference;

        LoadAppsTask(ThermalFragment context) {
            this.fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            ThermalFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) return;
            
            fragment.mPerAppCategory.removeAll();
        }

        @Override
        protected List<AppProfilePreference> doInBackground(Void... voids) {
            ThermalFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving() || fragment.getContext() == null) {
                return Collections.emptyList();
            }

            Context context = fragment.getContext();
            PackageManager pm = context.getPackageManager();
            List<AppProfilePreference> prefs = new ArrayList<>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));
            
            CharSequence[] entries = fragment.getResources().getTextArray(R.array.thermal_profile_entries);
            CharSequence[] entryValues = fragment.mProfileValues; // Usa o array já carregado

            for (ResolveInfo info : appList) {
                String packageName = info.activityInfo.packageName;
                CharSequence appName = info.loadLabel(pm);

                AppProfilePreference appPref = new AppProfilePreference(context);
                appPref.setKey(Constants.THERMAL_PREF_KEY_PREFIX + packageName);
                appPref.setTitle(appName);
                appPref.setIcon(info.loadIcon(pm));
                appPref.setEntries(entries);
                appPref.setEntryValues(entryValues);
                
                String currentValue = fragment.mSharedPrefs.getString(appPref.getKey(), Constants.THERMAL_DEFAULT_PROFILE_VALUE);
                appPref.setValue(currentValue);
 
                appPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    ThermalFragment strongFragment = fragmentReference.get();
                    if (strongFragment != null) {
                        String value = (String) newValue;
                        strongFragment.updatePreference((AppProfilePreference) preference, value);
                        strongFragment.mSharedPrefs.edit().putString(preference.getKey(), value).apply();
                    }
                    return true;
                });
 
                prefs.add(appPref);
            }
            return prefs;
        }

        @Override
        protected void onPostExecute(List<AppProfilePreference> prefs) {
            ThermalFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) return;

            for (Preference pref : prefs) {
                fragment.updatePreference((AppProfilePreference) pref, ((AppProfilePreference) pref).getValue());
                fragment.mPerAppCategory.addPreference(pref);
            }
        }
    }
}