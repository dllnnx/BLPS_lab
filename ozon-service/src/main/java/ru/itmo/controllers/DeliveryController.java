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
import ru.itmo.dto.requests.DeliveryPriceRequest;
import ru.itmo.dto.responses.DeliveryPriceResponse;
import ru.itmo.services.DeliveryService;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery Controller", description = "Контроллер для взаимодействия с доставкой")
public class DeliveryController {

    private final DeliveryService service;

    @PostMapping("/calculate")
    @Operation(
            summary = "Расчет доставки",
            description = "Возвращает цену доставки до адреса",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                               "address": "Каменноостровский проспект, 15"
                                             }
                                            """
                            )
                    )
            )
    )
    public DeliveryPriceResponse calculate(@Valid @RequestBody DeliveryPriceRequest request) {
        return service.calculateDeliveryPrice(request);
    }
}