/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings;

import android.content.Context;

import org.lineageos.settings.R;

public class Constants {

    // AutoHbm
    public static final String KEY_AUTO_HBM = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_AUTO_HBM_ENABLE_TIME = "auto_hbm_enable_time";
    public static final String KEY_AUTO_HBM_DISABLE_TIME = "auto_hbm_disable_time";
    public static final String KEY_CURRENT_LUX_LEVEL = "current_lux_level";
    public static String getHbmNode(Context context) {
        return context.getResources().getString(R.string.config_autoHbmNode);
    }
    public static final int DEFAULT_AUTO_HBM_THRESHOLD = 20000;

    // AutoDCDimming
    public static final String KEY_AUTO_DC_DIMMING = "auto_dc_dim";
    public static final String KEY_AUTO_DC_DIMMING_THRESHOLD = "auto_dc_dim_threshold";
    public static final String KEY_AUTO_DC_DIMMING_ENABLE_TIME = "auto_dc_dim_enable_time";
    public static final String KEY_AUTO_DC_DIMMING_DISABLE_TIME = "auto_dc_dim_disable_time";
    public static String getDcDimmingNode(Context context) {
        return context.getResources().getString(R.string.config_autoDcDimmingNode);
    }
    public static final int DEFAULT_AUTO_DC_DIMMING_THRESHOLD = 50;

    // Clear Speaker
    public static final String KEY_CLEAR_SPEAKER = "clear_speaker_pref";

    // Brightness
    public static final String KEY_CURRENT_BRIGHTNESS_LEVEL = "current_brightness_level";

    // Saturation
    public static final String KEY_SATURATION = "saturation";
    public static final String KEY_SATURATION_PREVIEW = "saturation_preview";
}
