package com.silo.common.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
class GoogleVisionTextExtractor implements TextExtractor {

    private final RestClient restClient;
    private final String apiKey;

    GoogleVisionTextExtractor(@Value("${silo.vision.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create("https://vision.googleapis.com");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> extractText(String imageUrl) {
        Map<String, Object> requestBody = Map.of("requests", List.of(Map.of(
                "image", Map.of("source", Map.of("imageUri", imageUrl)),
                "features", List.of(Map.of("type", "TEXT_DETECTION")))));

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/images:annotate?key={apiKey}", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> responses = (List<Map<String, Object>>) response.get("responses");
            if (responses == null || responses.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> fullTextAnnotation = (Map<String, Object>) responses.get(0).get("fullTextAnnotation");
            if (fullTextAnnotation == null) {
                return Optional.empty();
            }

            String text = (String) fullTextAnnotation.get("text");
            return Optional.ofNullable(text).filter(t -> !t.isBlank());
        } catch (Exception ex) {
            log.warn("OCR text extraction failed for {}: {}", imageUrl, ex.getMessage());
            return Optional.empty();
        }
    }
}
