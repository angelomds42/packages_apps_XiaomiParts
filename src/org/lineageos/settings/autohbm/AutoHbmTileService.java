
/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.autohbm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
 
import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;
 
public class AutoHbmTileService extends TileService {
 
    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTile(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, enabled).commit();
         
        Intent intent = new Intent(this, AutoHbmService.class);
        if (enabled) {
            startService(intent);
        } else {
            stopService(intent);
        }
         
        updateTile(enabled);
    }

    private void updateTile(boolean enabled) {
        final Tile tile = getQsTile();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        String subtitle = enabled ? getString(R.string.tile_on) : getString(R.string.tile_off);
        tile.setSubtitle(subtitle);
        tile.updateTile();
    }
}
 