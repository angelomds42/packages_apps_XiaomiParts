// Copyright (C) 2020-2025 The LineageOS Project
// SPDX-License-Identifier: Apache-2.0

package org.lineageos.settings.thermal;

import android.os.Bundle;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class ThermalActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = "Thermal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                new ThermalFragment(), TAG).commit();
    }
}