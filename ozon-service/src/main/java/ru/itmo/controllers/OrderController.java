package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.dto.requests.CreateOrderRequest;
import ru.itmo.dto.requests.UpdateOrderStatusRequest;
import ru.itmo.dto.responses.CreateOrderResponse;
import ru.itmo.dto.responses.OrderResponse;
import ru.itmo.security.AppUserPrincipal;
import ru.itmo.services.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Контроллер для создания заказов")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @Operation(
            summary = "Создание заказа",
            description = "Создает заказ на оплату и возвращает идентификаторы заказа и оплаты",
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
    public CreateOrderResponse createOrder(
            @AuthenticationPrincipal AppUserPrincipal user,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(user, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORDER_VIEW_OWN','ORDER_VIEW_PICKUP_POINT','ORDER_VIEW_ALL')")
    @Operation(
            summary = "Список заказов",
            description = "Список заказов в соответствии с ролью (свои / по ПВЗ / все)"
    )
    public List<OrderResponse> getOrders(Authentication authentication) {
        return orderService.getOrders(authentication);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority('ORDER_CANCEL_OWN')")
    @Operation(summary = "Отмена заказа", description = "Доступно для заказа в статусе ошибки оплаты; платёж переводится в INVALID")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable Long orderId
    ) {
        orderService.cancelOwnOrder(orderId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{orderId}/issue")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_PICKUP_POINT_STATUS')")
    @Operation(summary = "Выдача заказа в ПВЗ", description = "Перевод из PAID в ISSUED для заказа своего ПВЗ")
    public ResponseEntity<Void> markIssued(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable Long orderId
    ) {
        orderService.markIssuedAtPickup(orderId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_ALL')")
    @Operation(summary = "Изменение статуса заказа (админ)", description = "Произвольная смена статуса заказа")
    public ResponseEntity<Void> updateStatusAdmin(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        orderService.updateOrderStatusByAdmin(orderId, request.getOrderStatus());
        return ResponseEntity.noContent().build();
    }
}
