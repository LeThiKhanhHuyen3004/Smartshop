package com.example.app_technical_admin.model;

import android.content.Context;
import android.content.SharedPreferences;

public class PromotionStatusTracker {
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "PromotionStatusPrefs";
    private static final String KEY_PROMOTION = "promotion_";

    public PromotionStatusTracker(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasPromotionChanged(int promotionId, String startDate) {
        String key = KEY_PROMOTION + promotionId;
        String storedStartDate = sharedPreferences.getString(key, null);

        if (storedStartDate == null || !storedStartDate.equals(startDate)) {
            // Lưu startDate mới
            sharedPreferences.edit().putString(key, startDate).apply();
            return true;
        }
        return false;
    }
}