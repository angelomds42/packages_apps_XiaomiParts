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
    public static final String KEY_AUTO_HBM_ACTIVATION_THRESHOLD = "auto_hbm_activation_threshold";
    public static final String KEY_AUTO_HBM_DEACTIVATION_THRESHOLD = "auto_hbm_deactivation_threshold";
    public static final String KEY_AUTO_HBM_DELAY = "auto_hbm_transition_delay";
    public static final String KEY_CURRENT_LUX_LEVEL = "current_lux_level";

    public static String getHbmNode(Context context) {
        return context.getResources().getString(R.string.config_autoHbmNode);
    }

    public static int getDefaultAutoHbmActivationThreshold(Context context) {
        return context.getResources().getInteger(R.integer.config_autoHbmActivationThreshold);
    }

    public static int getDefaultAutoHbmDeactivationThreshold(Context context) {
        return context.getResources().getInteger(R.integer.config_autoHbmDeactivationThreshold);
    }

    // AutoDCDimming
    public static final String KEY_AUTO_DC_DIMMING = "auto_dc_dim";
    public static final String KEY_AUTO_DC_DIMMING_THRESHOLD = "auto_dc_dim_threshold";

    public static String getDcDimmingNode(Context context) {
        return context.getResources().getString(R.string.config_autoDcDimmingNode);

    }

    public static int getDefaultAutoDcDimmingThreshold(Context context) {
        return context.getResources().getInteger(R.integer.config_autoDcDimmingThreshold);
    }

    // Clear Speaker
    public static final String KEY_CLEAR_SPEAKER = "clear_speaker_pref";

    // Brightness
    public static final String KEY_CURRENT_BRIGHTNESS_LEVEL = "current_brightness_level";

    // Thermal
    public static final String KEY_THERMAL_MAIN_SWITCH = "thermal_main_switch";
    public static final String THERMAL_PREF_KEY_PREFIX = "thermal_profile_";
    public static final String THERMAL_DEFAULT_PROFILE_VALUE = "0";
    public static final String KEY_THERMAL_AUTO_SELECTION = "thermal_auto_selection_switch";
    public static boolean isThermalSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_thermalSupported);
    }

    // Saturation
    public static final String KEY_SATURATION = "saturation";
    public static final String KEY_SATURATION_PREVIEW = "saturation_preview";
}