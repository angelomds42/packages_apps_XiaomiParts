// Copyright (C) 2024 Paranoid Android
// SPDX-License-Identifier: Apache-2.0

package org.lineageos.settings.speaker;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.preference.PreferenceFragmentCompat;
import com.android.settingslib.widget.MainSwitchPreference;
import org.lineageos.settings.Constants;
import org.lineageos.settings.R;
import java.io.IOException;

public class ClearSpeakerFragment extends PreferenceFragmentCompat implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "ClearSpeakerFragment";
    private static final int PLAY_DURATION_MS = 30000;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer mMediaPlayer;
    private MainSwitchPreference mClearSpeakerPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.clear_speaker, rootKey);

        mClearSpeakerPref = findPreference(Constants.KEY_CLEAR_SPEAKER);
        mClearSpeakerPref.addOnSwitchChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked && startPlaying()) {
            mHandler.postDelayed(this::stopPlaying, PLAY_DURATION_MS);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlaying();
    }

    private boolean startPlaying() {
        if (getActivity() == null) return false;
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mMediaPlayer.setLooping(true);

        try (AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.clear_speaker_sound)) {
            mMediaPlayer.setDataSource(afd);
            mMediaPlayer.setVolume(1.0f, 1.0f);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mClearSpeakerPref.setEnabled(false);
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to play cleaning sound!", e);
            stopPlaying();
            return false;
        }
        return true;
    }

    private void stopPlaying() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        if (mClearSpeakerPref != null) {
            mClearSpeakerPref.setEnabled(true);
            mClearSpeakerPref.setChecked(false);
        }
    }
}