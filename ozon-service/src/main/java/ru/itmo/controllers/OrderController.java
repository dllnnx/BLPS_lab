package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.dto.responses.CreateOrderResponse;
import ru.itmo.dto.responses.OrderResponse;
import ru.itmo.services.OrderService;

import java.util.List;

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
                                               "delivery_address": "Кронверкский проспект, 49",
                                               "amount_kopecks": "100000"
                                             }
                                            """
                            )
                    )
            )
    )
    public CreateOrderResponse calculate(@AuthenticationPrincipal User user, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(user, request);
    }

    @GetMapping
    @Operation(
            summary = "Список заказов",
            description = "Список заказов текущего пользователя"
    )
    public List<OrderResponse> getOrders(@AuthenticationPrincipal User user){
        return orderService.getOrders(user);
    }
}
