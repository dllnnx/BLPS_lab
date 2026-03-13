package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.dto.requests.DeliveryPriceRequest;
import ru.itmo.dto.responses.CreateOrderResponse;
import ru.itmo.dto.responses.DeliveryPriceResponse;
import ru.itmo.services.OrderService;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Контроллер для создания заказов")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(
            summary = "Создание заказа",
            description = "Создает заказ на оплату и возвращает идентификатор оплаты",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                               "pickup_point_id": "1",
                                               "delivery_address": "Кронверкский проспект, 49"
                                             }
                                            """
                            )
                    )
            )
    )
    public CreateOrderResponse calculate(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
