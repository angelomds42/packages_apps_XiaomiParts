package org.lineageos.settings.autodcdim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;

public class AutoDcDimmingTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTile(sharedPrefs.getBoolean(Constants.KEY_AUTO_DC_DIMMING, false));
    }

     @Override
     public void onClick() {
         super.onClick();
         SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
         final boolean enabled = !(sharedPrefs.getBoolean(Constants.KEY_AUTO_DC_DIMMING, false));
         sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_DC_DIMMING, enabled).apply();

         Intent intent = new Intent(this, AutoDcDimmingService.class);
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
