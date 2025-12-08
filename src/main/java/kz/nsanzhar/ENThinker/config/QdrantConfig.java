package kz.nsanzhar.ENThinker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.apiKey}")
    private String qdrantApiKey;

    @Value("${qdrant.collection}")
    private String collection;

    @Bean("qdrantClient")
    public WebClient qdrantClient() {
        return WebClient.builder()
                .baseUrl(qdrantUrl)
                .defaultHeader("api-key", qdrantApiKey)
                .build();
    }

    @Bean
    public ApplicationRunner initQdrantCollection(WebClient qdrantClient) {
        return args -> {
            try {
                // Проверяем существует ли коллекция
                Boolean exists = qdrantClient.get()
                        .uri("/collections/" + collection)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> true)
                        .onErrorReturn(false)
                        .block();

                if (Boolean.FALSE.equals(exists)) {
                    // Создаем коллекцию с размерностью 768 для text-embedding-004
                    var collectionConfig = Map.of(
                            "vectors", Map.of(
                                    "size", 768,
                                    "distance", "Cosine"
                            )
                    );

                    qdrantClient.put()
                            .uri("/collections/" + collection)
                            .bodyValue(collectionConfig)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();

                    System.out.println("✅ Коллекция " + collection + " создана успешно!");
                } else {
                    System.out.println("✅ Коллекция " + collection + " уже существует");
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка при создании коллекции: " + e.getMessage());
            }
        };
    }
}
