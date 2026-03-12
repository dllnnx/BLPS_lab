package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.dto.requests.NearestPickupPointRequest;
import ru.itmo.dto.responses.PickupPointResponse;
import ru.itmo.services.PickupPointService;

@RestController
@RequestMapping("/api/pickup-points")
@RequiredArgsConstructor
@Tag(name = "Pickup Point Controller", description = "Контроллер для взаимодействия с ПВЗ")
public class PickupPointController {

    private final PickupPointService service;

    @PostMapping("/nearest")
    @Operation(
            summary = "Поиск ближайших ПВЗ",
            description = "Возвращает список ближайших ПВЗ",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                               "city": "Санкт-Петербург",
                                               "lat": 59.820200,
                                               "lng": 30.327500,
                                               "page": 0,
                                               "size": 10
                                             }
                                            """
                            )
                    )
            )
    )
    public Page<PickupPointResponse> findNearest(@Valid @RequestBody NearestPickupPointRequest request) {
        return service.findNearest(request);
    }
}
