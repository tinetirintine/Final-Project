package com.example.finalproject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtils {
    // A constant salt (pepper) to increase security against rainbow tables
    private static final String SALT = "NoteableApp_Secret_Salt_2024";

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Combine password with salt before hashing
            String saltedPassword = SALT + password;
            byte[] hash = digest.digest(saltedPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback to plain if algorithm not found (unlikely)
        }
    }
}