package lk.ijse.edu.golankacourier.service;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lk.ijse.edu.golankacourier.dto.payment.PaymentInitDto;
import lk.ijse.edu.golankacourier.entity.PaymentTransaction;

import java.util.Map;

public interface PaymentService {
    /**
     * Initialize a payment for a parcel (create PaymentTransaction and return provider payload).
     * Returns a map that the controller will return to the client (e.g., payhere payload or URL).
     */
    Map<String, Object> initializePayment(PaymentInitDto dto, Long requesterId);

    /**
     * Handle incoming webhook payload from payment provider.
     */
    void handleProviderWebhook(Map<String, Object> payload);
}
