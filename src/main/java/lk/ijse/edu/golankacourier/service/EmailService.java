package lk.ijse.edu.golankacourier.service;

/**
 * --------------------------------------------
 *
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 9/21/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

public interface EmailService {
    void sendVerificationEmail(String toEmail, String toName, String verificationCode);
    void sendDriverApplicationResult(String toEmail, String toName, boolean approved, String notes);
    void sendPasswordResetEmail(String toEmail, String toName, String token);
    void sendPaymentReceipt(String toEmail, String toName, String merchantRef, String amount);
}

