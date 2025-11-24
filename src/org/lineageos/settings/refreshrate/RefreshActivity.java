package org.lineageos.settings.refreshrate;

import android.os.Bundle;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import org.lineageos.settings.R;

public class RefreshActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG = "RefreshRate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                new RefreshFragment(), TAG).commit();
    }
}
