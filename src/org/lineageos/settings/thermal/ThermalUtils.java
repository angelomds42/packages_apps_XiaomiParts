/*
 * Copyright (C) 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public final class ThermalUtils {

    private final SharedPreferences mSharedPrefs;
    private final Context mContext;
    private final String[] mThermalProfileValues;
    private final String mThermalNode;

    protected ThermalUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mThermalProfileValues = context.getResources().getStringArray(R.array.thermal_profile_values);
        mThermalNode = context.getResources().getString(R.string.config_thermalNode);
    }

    public void setAutoThermalProfile(String packageName) {
        String profileValue = getProfileForPackageCategory(packageName);
        FileUtils.writeValue(mThermalNode, profileValue);
    }

    private String getProfileForPackageCategory(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            switch (appInfo.category) {
                case ApplicationInfo.CATEGORY_GAME:
                    return mThermalProfileValues[5];

                case ApplicationInfo.CATEGORY_VIDEO:
                case ApplicationInfo.CATEGORY_AUDIO:
                    return mThermalProfileValues[6];

                default:
                    if (packageName.contains("browser") || packageName.contains("chrome") || packageName.contains("firefox")) {
                        return mThermalProfileValues[2];
                    }
                    return mThermalProfileValues[0];
            }
        } catch (PackageManager.NameNotFoundException e) {
            return mThermalProfileValues[0];
        }
    }

    public static boolean isThermalSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_thermalSupported);
    }

    public static void startService(Context context) {
        if (isThermalSupported(context)) {
            context.startServiceAsUser(new Intent(context, ThermalService.class),
                    UserHandle.CURRENT);
        }
    }

    public void setThermalProfile(String packageName) {
        String profileValue = mSharedPrefs.getString(
                Constants.THERMAL_PREF_KEY_PREFIX + packageName, Constants.THERMAL_DEFAULT_PROFILE_VALUE);
 
        String state = mThermalProfileValues[0];
        boolean valueFound = false;
        for (String value : mThermalProfileValues) {
            if (value.equals(profileValue)) {
                state = value;
                valueFound = true;
                break;
            }
        }
 
        FileUtils.writeValue(mThermalNode, state);
    }

    public void setDefaultThermalProfile() {
        String defaultState = mThermalProfileValues[0];
        FileUtils.writeValue(mThermalNode, defaultState);
    }
}