package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.dto.queue.PaymentWithStatusDto;
import ru.itmo.dto.requests.PayRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.dto.responses.PaymentStatusResponse;
import ru.itmo.models.CardInfo;
import ru.itmo.models.Payment;
import ru.itmo.models.PaymentStatus;
import ru.itmo.producers.PaymentProducer;
import ru.itmo.repositories.CardInfoRepository;
import ru.itmo.repositories.PaymentRepository;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardInfoRepository cardInfoRepository;
    private final PaymentProducer paymentProducer;

    @Transactional(rollbackFor = Exception.class)
    public CreatePaymentResponse createPayment(Long amount) {
        Payment payment = paymentRepository.save(new Payment(
                UUID.randomUUID(),
                amount,
                PaymentStatus.PENDING
        ));
        return new CreatePaymentResponse(payment.getId());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> new PaymentStatusResponse(p.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void pay(PayRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not pending");
        }

        CardInfo cardInfo = cardInfoRepository.findCardInfoByCardId(request.getCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        boolean cardInvalid = !Objects.equals(cardInfo.getMonth(), request.getMonth()) ||
                !Objects.equals(cardInfo.getYear(), request.getYearTail()) ||
                !Objects.equals(cardInfo.getCvc(), request.getCvc());
        if (cardInvalid) {
            markFailed(payment);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card format");
        }
        if (payment.getAmountKopecks() > cardInfo.getBalanceKopecks()) {
            markFailed(payment);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        cardInfo.setBalanceKopecks(cardInfo.getBalanceKopecks() - payment.getAmountKopecks());
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
        paymentProducer.sendPaymentStatus(new PaymentWithStatusDto(payment.getId(), PaymentStatus.COMPLETED));
        cardInfoRepository.save(cardInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void invalidatePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only failed payments can be invalidated"
            );
        }
        payment.setStatus(PaymentStatus.INVALID);
        paymentRepository.save(payment);
        paymentProducer.sendPaymentStatus(new PaymentWithStatusDto(payment.getId(), PaymentStatus.FAILED));
    }

    private void markFailed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        paymentProducer.sendPaymentStatus(new PaymentWithStatusDto(payment.getId(), PaymentStatus.FAILED));
    }
}
