package ru.itmo.clients;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.dto.middleware.Coordinates;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeocoderClient {
    @Value("${secrets.geocoder:}")
    private String yandexMapsApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<Coordinates> getCoordinates(String address) {
        String url = String.format(
                "https://geocode-maps.yandex.ru/v1/?apikey=%s&geocode=%s&format=json",
                yandexMapsApiKey, address
        );
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> geoResponse = (Map<String, Object>) response.get("response");
            Map<String, Object> geoObjectCollection = (Map<String, Object>) geoResponse.get("GeoObjectCollection");
            List<Map<String, Object>> featureMember = (List<Map<String, Object>>) geoObjectCollection.get("featureMember");

            if (!featureMember.isEmpty()) {
                Map<String, Object> geoObject = (Map<String, Object>) featureMember.get(0).get("GeoObject");
                Map<String, Object> metaDataProperty = (Map<String, Object>) geoObject.get("metaDataProperty");
                Map<String, Object> geocoderMetaData = (Map<String, Object>) metaDataProperty.get("GeocoderMetaData");
                String precision = (String) geocoderMetaData.get("precision");
                String kind = (String) geocoderMetaData.get("kind");

                boolean preciseEnough = ("exact".equals(precision) || "number".equals(precision))
                        && ("house".equals(kind) || "street".equals(kind));
                if (!preciseEnough) {
                    System.err.println("Address too imprecise: precision=" + precision + ", kind=" + kind);
                    return Optional.empty();
                }

                Map<String, Object> point = (Map<String, Object>) geoObject.get("Point");
                String pos = (String) point.get("pos"); // "25.197300 55.274243"

                String[] parts = pos.split(" ");
                double longitude = Double.parseDouble(parts[0]);
                double latitude = Double.parseDouble(parts[1]);

                Coordinates coordinates = new Coordinates();
                coordinates.setLatitude(latitude);
                coordinates.setLongitude(longitude);

                return Optional.of(coordinates);
            }

        } catch (Exception e) {
            System.err.println("Error calling Yandex Maps API: " + e.getMessage());
        }
        return Optional.empty();
    }
}
