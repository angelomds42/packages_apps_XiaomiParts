/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.lineageos.settings.autohbm.AutoHbmActivity;
import org.lineageos.settings.autohbm.AutoHbmFragment;
import org.lineageos.settings.autohbm.AutoHbmTileService;
import org.lineageos.settings.saturation.SaturationFragment;
import org.lineageos.settings.utils.ComponentUtils;
import org.lineageos.settings.utils.FileUtils;

public class Startup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        // Auto hbm
        AutoHbmFragment.toggleAutoHbmService(context);

        ComponentUtils.toggleComponent(
                context,
                AutoHbmActivity.class,
                AutoHbmFragment.isHbmSupported(context)
        );

        ComponentUtils.toggleComponent(
                context,
                AutoHbmTileService.class,
                AutoHbmFragment.isHbmSupported(context)
        );

        // Saturation
        SaturationFragment saturationFragment = new SaturationFragment();
        saturationFragment.restoreSaturationSetting(context);
    }
}
