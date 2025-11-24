package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;

public final class RefreshUtils {

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    private float mDefaultMinRate;
    private float mDefaultPeakRate;
    private boolean mIsOverridden = false;

    public RefreshUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        syncSystemValues();
    }

    public void startService() {
        mContext.startServiceAsUser(new Intent(mContext, RefreshService.class),
                UserHandle.CURRENT);
    }

    private void syncSystemValues() {
        if (!mIsOverridden) {
            mDefaultMinRate = Settings.System.getFloat(mContext.getContentResolver(),
                    Constants.KEY_MIN_REFRESH_RATE, 60.0f);
            mDefaultPeakRate = Settings.System.getFloat(mContext.getContentResolver(),
                    Constants.KEY_PEAK_REFRESH_RATE, 60.0f);
        }
    }

    public void setRefreshProfile(String packageName) {
        String profileValue = mSharedPrefs.getString(
                Constants.REFRESH_PREF_KEY_PREFIX + packageName,
                Constants.REFRESH_DEFAULT_PROFILE_VALUE);

        if (Constants.REFRESH_DEFAULT_PROFILE_VALUE.equals(profileValue)) {
            restoreDefaultProfile();
            return;
        }

        syncSystemValues();

        try {
            float targetRate = Float.parseFloat(profileValue);

            Settings.System.putFloat(mContext.getContentResolver(), Constants.KEY_MIN_REFRESH_RATE, targetRate);
            Settings.System.putFloat(mContext.getContentResolver(), Constants.KEY_PEAK_REFRESH_RATE, targetRate);

            mIsOverridden = true;
        } catch (NumberFormatException e) {
            restoreDefaultProfile();
        }
    }

    public void restoreDefaultProfile() {
        if (mIsOverridden) {
            Settings.System.putFloat(mContext.getContentResolver(), Constants.KEY_MIN_REFRESH_RATE, mDefaultMinRate);
            Settings.System.putFloat(mContext.getContentResolver(), Constants.KEY_PEAK_REFRESH_RATE, mDefaultPeakRate);
            mIsOverridden = false;
        } else {
            syncSystemValues();
        }
    }
}
