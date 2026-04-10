package ru.itmo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.itmo.Coordinates;
import ru.itmo.clients.GeocoderClient;
import ru.itmo.security.AppUserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Hello World Controller", description = "Тестовый контроллер")
public class HelloWorldController {
    private final GeocoderClient geocoderClient;

    @GetMapping("/public/ping")
    @Operation(summary = "Helthcheck")
    public String ping() {
        return "pong";
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь", description = "Возвращает username текущего пользователя")
    // note: так же можно воспользоваться SecurityContextHolder.getContext().getAuthentication().getName();
    public ResponseEntity<String> me(@AuthenticationPrincipal AppUserPrincipal user) {
        return ResponseEntity.ok(user.getUsername());
    }

    @GetMapping("/fetch-cords")
    @Operation(summary = "Проверить соединение с геокодером")
    public ResponseEntity<Coordinates> getCoordinates() {
        return ResponseEntity.ok(geocoderClient.getCoordinates("кронверкский 49").get());
    }
}