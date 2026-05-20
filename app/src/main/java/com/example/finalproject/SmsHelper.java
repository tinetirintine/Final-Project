package com.example.finalproject;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsHelper {

    public interface SmsCallback {
        void onSmsSent(boolean success, String errorMessage);
    }

    public static void sendVerificationSMS(Context context, String phoneNumber, String code, SmsCallback callback) {
        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            
            String message = "Your Noteable verification code is: " + code;
            
            if (smsManager != null) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.d("SmsHelper", "SMS sent to " + phoneNumber);
                if (callback != null) callback.onSmsSent(true, null);
            } else {
                if (callback != null) callback.onSmsSent(false, "SmsManager not available");
            }
            
        } catch (Exception e) {
            Log.e("SmsHelper", "Failed to send SMS: " + e.getMessage());
            if (callback != null) callback.onSmsSent(false, e.getMessage());
        }
    }
}
