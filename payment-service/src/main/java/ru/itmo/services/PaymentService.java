package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.dto.requests.PayRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.dto.responses.PaymentStatusResponse;
import ru.itmo.models.CardInfo;
import ru.itmo.models.Payment;
import ru.itmo.models.PaymentStatus;
import ru.itmo.repositories.CardInfoRepository;
import ru.itmo.repositories.PaymentRepository;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardInfoRepository cardInfoRepository;

    public CreatePaymentResponse createPayment(Long amount) {
        Payment payment = paymentRepository.save(new Payment(
                UUID.randomUUID(),
                amount,
                PaymentStatus.PENDING
        ));
        return new CreatePaymentResponse(payment.getId());
    }

    public PaymentStatusResponse getPaymentStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> new PaymentStatusResponse(p.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    public void pay(PayRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!payment.getStatus().equals(PaymentStatus.PENDING))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not pending");

        CardInfo cardInfo = cardInfoRepository.findCardInfoByCardId(request.getCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        if (
                !Objects.equals(cardInfo.getMonth(), request.getMonth()) ||
                        !Objects.equals(cardInfo.getYear(), request.getYearTail()) ||
                        !Objects.equals(cardInfo.getCvc(), request.getCvc())
        )
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card format");
        if (payment.getAmountKopecks() > cardInfo.getBalanceKopecks())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        cardInfo.setBalanceKopecks(cardInfo.getBalanceKopecks() - payment.getAmountKopecks());
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
        cardInfoRepository.save(cardInfo);
    }
}
