package com.example.finalproject;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailHelper {

    // IMPORTANT: You need to use a real email and an "App Password" (not your regular password)
    // To get an App Password for Gmail:
    // 1. Enable 2-Step Verification in your Google Account.
    // 2. Go to Security > App passwords.
    // 3. Select 'Mail' and 'Other' (Android), then generate.
    private static final String[] SENDER_EMAILS = {"jfontillaz.9008@umak.edu.ph", "cdelacruz.8946@umak.edu.ph"};
    private static final String[] SENDER_PASSWORDS = {"gsxeibcqxmqnchhg", "rclnewssdeoypjmn"};


    public interface EmailCallback {
        void onEmailSent(boolean success, String errorMessage);
    }

    public static void sendVerificationCode(String recipientEmail, String code, EmailCallback callback) {
        new Thread(() -> {
            String lastError = "Unknown error";
            Log.d("EmailHelper", "Attempting to send code to: " + recipientEmail);
            for (int i = 0; i < SENDER_EMAILS.length; i++) {
                final String senderEmail = SENDER_EMAILS[i];
                final String senderPassword = SENDER_PASSWORDS[i];
                try {
                    Log.d("EmailHelper", "Using sender: " + senderEmail);
                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.starttls.required", "true");
                    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.port", "587");
                    props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");

                    Session session = Session.getInstance(props);

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail, "Noteable App"));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()));
                    message.setSubject("Noteable - Verification Code");
                    message.setText("Your verification code is: " + code + "\n\nPlease enter this code in the app to proceed.");

                    Transport transport = session.getTransport("smtp");
                    transport.connect("smtp.gmail.com", senderEmail, senderPassword);
                    transport.sendMessage(message, message.getAllRecipients());
                    transport.close();

                    Log.d("EmailHelper", "Email sent successfully via " + senderEmail);
                    if (callback != null) callback.onEmailSent(true, null);
                    return; // Exit if successful

                } catch (Exception e) {
                    lastError = e.getMessage();
                    Log.e("EmailHelper", "Error sending email via " + senderEmail + ": " + lastError);
                    e.printStackTrace();
                }
            }
            // If we reach here, all accounts failed
            if (callback != null) callback.onEmailSent(false, lastError);
        }).start();
    }
}
