package org.lineageos.settings.refreshrate;

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

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.thermal.AppProfilePreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RefreshFragment extends PreferenceFragmentCompat
        implements CompoundButton.OnCheckedChangeListener {

    private MainSwitchPreference mMainSwitch;
    private PreferenceCategory mPerAppCategory;
    private SharedPreferences mSharedPrefs;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.refresh_profiles, rootKey);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mMainSwitch = findPreference(Constants.KEY_REFRESH_MAIN_SWITCH);
        mMainSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_REFRESH_MAIN_SWITCH, false));
        mMainSwitch.addOnSwitchChangeListener(this);

        mPerAppCategory = findPreference("refresh_per_app_category");

        loadApps();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_REFRESH_MAIN_SWITCH, isChecked).apply();

        Intent intent = new Intent(getContext(), RefreshService.class);
        if (isChecked) {
            getContext().startServiceAsUser(intent, UserHandle.CURRENT);
        } else {
            getContext().stopServiceAsUser(intent, UserHandle.CURRENT);
        }
    }

    private void updatePreference(AppProfilePreference pref, String value) {
        int index = pref.findIndexOfValue(value);
        if (index != -1) {
            pref.setSummary(pref.getEntries()[index]);
        }
    }

    private void loadApps() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Context context = getContext();
            if (context == null)
                return;

            PackageManager pm = context.getPackageManager();
            List<AppProfilePreference> prefs = new ArrayList<>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

            CharSequence[] entries = getResources().getTextArray(R.array.refresh_profile_entries);
            CharSequence[] entryValues = getResources().getTextArray(R.array.refresh_profile_values);

            for (ResolveInfo info : appList) {
                String packageName = info.activityInfo.packageName;
                CharSequence appName = info.loadLabel(pm);

                AppProfilePreference appPref = new AppProfilePreference(context);
                appPref.setKey(Constants.REFRESH_PREF_KEY_PREFIX + packageName);
                appPref.setTitle(appName);
                appPref.setIcon(info.loadIcon(pm));
                appPref.setEntries(entries);
                appPref.setEntryValues(entryValues);
                appPref.setDefaultValue(Constants.REFRESH_DEFAULT_PROFILE_VALUE);

                String currentValue = mSharedPrefs.getString(appPref.getKey(), Constants.REFRESH_DEFAULT_PROFILE_VALUE);
                appPref.setValue(currentValue);
                updatePreference(appPref, currentValue);

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
                        mPerAppCategory.addPreference(pref);
                    }
                }
            });
        });
    }
}
