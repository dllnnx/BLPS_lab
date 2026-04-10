package ru.itmo;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeoService {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String yandexMapsApiKey;

    public GeoService() {
        this.yandexMapsApiKey = System.getenv("YANDEX_MAPS_API_KEY");
        if (yandexMapsApiKey == null || yandexMapsApiKey.isEmpty()) {
            throw new IllegalStateException("YANDEX_MAPS_API_KEY environment variable is not set");
        }
    }

    public Coordinates getCoordinates(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = String.format(
                "https://geocode-maps.yandex.ru/1.x/?apikey=%s&geocode=%s&format=json",
                yandexMapsApiKey, encodedAddress
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(
                        response.body(), Map.class);

                Map<String, Object> geoResponse = (Map<String, Object>) responseMap.get("response");
                Map<String, Object> geoObjectCollection = (Map<String, Object>) geoResponse.get("GeoObjectCollection");
                List<Map<String, Object>> featureMember = (List<Map<String, Object>>) geoObjectCollection.get("featureMember");

                if (!featureMember.isEmpty()) {
                    Map<String, Object> geoObject = (Map<String, Object>) featureMember.get(0).get("GeoObject");
                    Map<String, Object> point = (Map<String, Object>) geoObject.get("Point");
                    String pos = (String) point.get("pos");

                    String[] parts = pos.split(" ");
                    double longitude = Double.parseDouble(parts[0]);
                    double latitude = Double.parseDouble(parts[1]);

                    Coordinates coordinates = new Coordinates();
                    coordinates.setLatitude(latitude);
                    coordinates.setLongitude(longitude);
                    return coordinates;
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Yandex Maps API: " + e.getMessage());
        }
        return null;
    }
}
