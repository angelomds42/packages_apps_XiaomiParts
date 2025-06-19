package org.lineageos.settings.refreshrate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Display;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RefreshTileService extends TileService {

    private Context context;
    private Tile tile;

    private List<int[]> refreshRates;
    private int currentRateIndex = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        loadRefreshRates();
        syncFromSettings();
    }

    private void loadRefreshRates() {
        refreshRates = new ArrayList<>();
        Resources res = getResources();
        String[] rates = res.getStringArray(R.array.refresh_rate_tile_options);
        for (String rate : rates) {
            String[] values = rate.split(",");
            if (values.length == 2) {
                try {
                    int min = Integer.parseInt(values[0].trim());
                    int max = Integer.parseInt(values[1].trim());
                    refreshRates.add(new int[] { min, max });
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    private void syncFromSettings() {
        if (refreshRates.isEmpty()) {
            return;
        }

        float minRate = Settings.System.getFloat(context.getContentResolver(), Constants.KEY_MIN_REFRESH_RATE,
                refreshRates.get(0)[0]);
        float maxRate = Settings.System.getFloat(context.getContentResolver(), Constants.KEY_PEAK_REFRESH_RATE,
                refreshRates.get(0)[1]);

        currentRateIndex = 0;
        for (int i = 0; i < refreshRates.size(); i++) {
            if (refreshRates.get(i)[0] == (int) minRate && refreshRates.get(i)[1] == (int) maxRate) {
                currentRateIndex = i;
                break;
            }
        }
    }

    private void cycleRefreshRate() {
        if (refreshRates.isEmpty()) {
            return;
        }

        currentRateIndex = (currentRateIndex + 1) % refreshRates.size();

        float minRate = refreshRates.get(currentRateIndex)[0];
        float maxRate = refreshRates.get(currentRateIndex)[1];

        Settings.System.putFloat(context.getContentResolver(), Constants.KEY_MIN_REFRESH_RATE, minRate);
        Settings.System.putFloat(context.getContentResolver(), Constants.KEY_PEAK_REFRESH_RATE, maxRate);
    }

    private void updateTileView() {
        if (tile == null || refreshRates.isEmpty()) {
            if (tile != null) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel(getString(R.string.refresh_rate_title));
                tile.setSubtitle("N/A");
                tile.updateTile();
            }
            return;
        }

        String displayText;
        int min = refreshRates.get(currentRateIndex)[0];
        int max = refreshRates.get(currentRateIndex)[1];

        if (min == max) {
            displayText = String.format(Locale.US, "%d Hz", min);
        } else {
            displayText = String.format(Locale.US, "%d-%d Hz", min, max);
        }

        tile.setLabel(getString(R.string.refresh_rate_title));
        tile.setSubtitle(displayText);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_refresh_rate_tile));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
        syncFromSettings();
        updateTileView();
    }

    @Override
    public void onClick() {
        super.onClick();
        cycleRefreshRate();
        updateTileView();
    }
}