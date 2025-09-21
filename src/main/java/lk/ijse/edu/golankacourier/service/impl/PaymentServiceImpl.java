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

import lk.ijse.edu.golankacourier.dto.payment.PaymentInitDto;
import lk.ijse.edu.golankacourier.entity.Parcel;
import lk.ijse.edu.golankacourier.entity.PaymentTransaction;
import lk.ijse.edu.golankacourier.entity.User;
import lk.ijse.edu.golankacourier.repository.ParcelRepository;
import lk.ijse.edu.golankacourier.repository.PaymentRepository;
import lk.ijse.edu.golankacourier.repository.UserRepository;
import lk.ijse.edu.golankacourier.service.EmailService;
import lk.ijse.edu.golankacourier.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${payhere.merchant.id:}")
    private String payHereMerchantId;

    @Value("${payhere.api.base-url:https://sandbox.payhere.lk}")
    private String payhereBaseUrl;

    @Override
    @Transactional
    public Map<String, Object> initializePayment(PaymentInitDto dto, Long requesterId) {
        Parcel parcel = parcelRepository.findById(dto.getParcelId())
                .orElseThrow(() -> new IllegalArgumentException("Parcel not found: " + dto.getParcelId()));

        // Confirm requester is parcel owner (or staff)
        if (!parcel.getCustomer().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Not authorized to pay for this parcel");
        }

        // Persist a pending payment transaction
        PaymentTransaction tx = PaymentTransaction.builder()
                .merchantRef(parcel.getTrackingCode())
                .parcel(parcel)
                .customer(parcel.getCustomer())
                .amount(dto.getAmount().setScale(2, RoundingMode.HALF_UP))
                .currency(dto.getCurrency())
                .paymentMethod("PAYHERE")
                .status("PENDING")
                .build();
        paymentRepository.save(tx);

        // Build provider payload for PayHere (server should add merchant_id, return/notify URLs and signature)
        Map<String, Object> payload = new HashMap<>();
        payload.put("merchant_id", payHereMerchantId);
        payload.put("merchant_reference", parcel.getTrackingCode());
        payload.put("amount", tx.getAmount().toPlainString());
        payload.put("currency", tx.getCurrency());
        // add customer fields
        payload.put("first_name", dto.getCustomerName());
        payload.put("email", dto.getCustomerEmail());
        payload.put("phone", dto.getCustomerPhone());
        // TODO: add return_url and notify_url from config
        payload.put("return_url", dto.getReturnUrl() != null ? dto.getReturnUrl() : "/payments/return");
        payload.put("notify_url", "/api/v1/webhook/payhere");

        // TODO: compute and add PayHere signature/MD5 according to their docs

        Map<String, Object> result = new HashMap<>();
        result.put("paymentInitPayload", payload);
        result.put("paymentTransactionId", tx.getId());
        return result;
    }

    @Override
    @Transactional
    public void handleProviderWebhook(Map<String, Object> payload) {
        // Example payload handling:
        // 1) verify signature using payhere secret (TODO)
        // 2) locate PaymentTransaction by merchant_reference or provider_txn_id
        // 3) update tx.status to PAID/FAILED and set providerTxnId, paidAt, rawPayload
        // 4) mark parcel status appropriately and send receipt email

        // NOTE: implementation depends on exact provider payload fields.
        String merchantRef = payload.getOrDefault("merchant_reference", payload.get("merchant_ref")) != null ?
                payload.getOrDefault("merchant_reference", payload.get("merchant_ref")).toString() : null;
        String status = payload.getOrDefault("status", "UNKNOWN").toString();

        if (merchantRef == null) {
            throw new IllegalArgumentException("Missing merchant reference in webhook payload");
        }

        Optional<PaymentTransaction> maybe = paymentRepository.findByMerchantRef(merchantRef);
        if (maybe.isEmpty()) {
            // log and ignore unknown payment
            return;
        }
        PaymentTransaction tx = maybe.get();

        // example mapping
        if ("paid".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            tx.setStatus("PAID");
        } else {
            tx.setStatus("FAILED");
        }
        tx.setProviderTxnId(payload.getOrDefault("payment_id", payload.get("provider_txn_id")).toString());
        tx.setRawPayload(payload.toString());
        paymentRepository.save(tx);

        // update parcel if paid
        Parcel parcel = tx.getParcel();
        if ("PAID".equalsIgnoreCase(tx.getStatus())) {
            parcel.setStatus("PAID");
            parcelRepository.save(parcel);
            // send receipt
            User c = tx.getCustomer();
            emailService.sendPaymentReceipt(c.getEmail(), c.getFullName(), tx.getMerchantRef(), tx.getAmount().toString());
        }
    }
}
