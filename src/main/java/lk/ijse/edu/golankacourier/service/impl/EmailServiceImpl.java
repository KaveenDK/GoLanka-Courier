package lk.ijse.edu.golankacourier.service.impl;

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

import lk.ijse.edu.golankacourier.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Minimal email service using JavaMailSender. For production, replace with templated emails (Thymeleaf).
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@golanka.lk}")
    private String from;

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String verificationCode) {
        String subject = "GoLanka — Verify your email";
        String text = "Hi " + toName + ",\n\nYour verification code: " + verificationCode +
                "\n\nIf you did not sign up, ignore this message.\n\nThanks,\nGoLanka Team";
        sendSimpleEmail(toEmail, subject, text);
    }

    @Override
    public void sendDriverApplicationResult(String toEmail, String toName, boolean approved, String notes) {
        String subject = approved ? "GoLanka — Driver Application Approved" : "GoLanka — Driver Application Update";
        String text;
        if (approved) {
            text = "Hi " + toName + ",\n\nCongratulations — your driver application was approved.\n\n" + notes +
                    "\n\nPlease log in to your driver account.\n\nThanks,\nGoLanka Team";
        } else {
            text = "Hi " + toName + ",\n\nThank you for applying. Your application was reviewed and the result is: Declined.\n\nNotes: "
                    + notes + "\n\nIf you want to re-apply, make sure to follow guidelines.\n\nThanks,\nGoLanka Team";
        }
        sendSimpleEmail(toEmail, subject, text);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String token) {
        String subject = "GoLanka — Password reset";
        String resetUrl = "/auth/reset?token=" + token; // ideally full URL in config
        String text = "Hi " + toName + ",\n\nTo reset your password click the link: " + resetUrl
                + "\nIf you didn't request this, ignore this email.\n\nThanks,\nGoLanka Team";
        sendSimpleEmail(toEmail, subject, text);
    }

    @Override
    public void sendPaymentReceipt(String toEmail, String toName, String merchantRef, String amount) {
        String subject = "GoLanka — Payment receipt";
        String text = "Hi " + toName + ",\n\nWe have received your payment for " + merchantRef + ".\nAmount: " + amount +
                "\n\nThank you for using GoLanka.\n\nRegards,\nGoLanka Team";
        sendSimpleEmail(toEmail, subject, text);
    }

    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
