// Copyright (C) 2024 Paranoid Android
// SPDX-License-Identifier: Apache-2.0

package org.lineageos.settings.speaker;

import android.os.Bundle;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class ClearSpeakerActivity extends CollapsingToolbarBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                new ClearSpeakerFragment()).commit();
    }
}