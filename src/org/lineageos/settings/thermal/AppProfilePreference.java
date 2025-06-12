package org.lineageos.settings.thermal;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;
import org.lineageos.settings.R;

public class AppProfilePreference extends ListPreference {

    private Drawable mProfileIcon;

    public AppProfilePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app_profile);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(true);

        ImageView profileIconView = (ImageView) holder.findViewById(R.id.profile_icon);
        if (mProfileIcon != null) {
            profileIconView.setImageDrawable(mProfileIcon);
            profileIconView.setVisibility(android.view.View.VISIBLE);
        } else {
            profileIconView.setVisibility(android.view.View.GONE);
        }
    }

    public void setProfileIcon(Drawable icon) {
        if (icon != mProfileIcon) {
            mProfileIcon = icon;
            notifyChanged();
        }
    }
}
