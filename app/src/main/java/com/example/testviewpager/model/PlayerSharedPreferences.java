package com.example.testviewpager.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE;

public class PlayerSharedPreferences {
    private static final String PLAYER_SHARED_PREFERENCES = "playerSettings";
    private static final String REPEAT_MODE_SHARED_PREFERENCE = "repeatModeSetting";
    private static SharedPreferences sharedPreferences;
    private static int repeatMode;

    public static void saveSettings(Context context) {
        sharedPreferences = context.getSharedPreferences(PLAYER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putInt(REPEAT_MODE_SHARED_PREFERENCE, repeatMode);
        ed.apply();
    }

    public static void loadSettings(Context context) {
        sharedPreferences = context.getSharedPreferences(PLAYER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        repeatMode = sharedPreferences.getInt(REPEAT_MODE_SHARED_PREFERENCE, REPEAT_MODE_NONE);
    }

    public static int getRepeatMode() {
        Log.d(PLAYER_SHARED_PREFERENCES, String.valueOf(repeatMode));
        return repeatMode;
    }

    public static void setRepeatMode(int repeatMode, Context context) {
        PlayerSharedPreferences.repeatMode = repeatMode;
        saveSettings(context);
        Log.d(PLAYER_SHARED_PREFERENCES, String.valueOf(repeatMode));
    }
}
