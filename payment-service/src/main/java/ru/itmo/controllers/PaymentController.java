package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.dto.requests.PayRequest;
import ru.itmo.dto.requests.CreatePaymentRequest;
import ru.itmo.dto.responses.CreatePaymentResponse;
import ru.itmo.dto.responses.PaymentStatusResponse;
import ru.itmo.services.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Платежный сервис")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(
            summary = "Создать счет",
            description = "Создает счет для оплаты и возвращает uuid",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"amountKopecks\": 10000}")
                    )
            )
    )
    public CreatePaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request.getAmountKopecks());
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Статус платежа", description = "Возвращает статус платежа по uuid")
    public PaymentStatusResponse getPaymentStatus(@PathVariable("paymentId") UUID paymentId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return response;
    }

    @PostMapping("/pay")
    @Operation(
            summary = "Оплатить счет",
            description = "Проводит транзакцию по оплате счета с карты",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                               "cardId": "4000000000000002",
                                               "month": 12,
                                               "yearTail": 29,
                                               "cvc": "111",
                                               "paymentId": "108d4c7c-b79a-42f7-bc30-456935fb1fd3"
                                             }
                                            """
                            )
                    )
            )
    )
    public ResponseEntity<Void> pay(@Valid @RequestBody PayRequest request) {
        paymentService.pay(request);
        return ResponseEntity.noContent().build();
    }
}
