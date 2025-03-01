package com.baconga.kttstore.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "KTTStorePrefs";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public PreferenceManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return preferences.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }
} 