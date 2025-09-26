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
    Map<String, Object> initializePayment(PaymentInitDto dto, Long requesterId);

    void handleProviderWebhook(Map<String, Object> payload);
}
