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
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.telecom.DefaultDialerManager;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

import java.util.List;

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

    private boolean isDefaultLauncher(String packageName) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return res != null && res.activityInfo != null && res.activityInfo.packageName.equals(packageName);
    }

    private String getProfileForPackageCategory(String packageName) {
        if (isDefaultLauncher(packageName)) {
            return mThermalProfileValues[0]; // Default
        }

        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            if ((appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0
                    || appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return mThermalProfileValues[5]; // Gaming
            }

            switch (appInfo.category) {
                case ApplicationInfo.CATEGORY_VIDEO:
                case ApplicationInfo.CATEGORY_AUDIO:
                    return mThermalProfileValues[6]; // Streaming
            }

            if (isBrowserApp(packageName)) {
                return mThermalProfileValues[2]; // Browser
            }

            if (isCameraApp(packageName)) {
                return mThermalProfileValues[3]; // Camera
            }

            if (DefaultDialerManager.getDefaultDialerApplication(mContext).equals(packageName)) {
                return mThermalProfileValues[4]; // Dialer
            }

            return mThermalProfileValues[0]; // Default

        } catch (PackageManager.NameNotFoundException e) {
            return mThermalProfileValues[0]; // Default
        }
    }

    private boolean isBrowserApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http:"));
        intent.setPackage(packageName);
        ResolveInfo info = mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null;
    }

    private boolean isCameraApp(String packageName) {
        Intent intent = new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.setPackage(packageName);
        List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfos != null && !resolveInfos.isEmpty();
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

        String state = mThermalProfileValues[0]; // Default
        for (String value : mThermalProfileValues) {
            if (value.equals(profileValue)) {
                state = value;
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