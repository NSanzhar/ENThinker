package kz.nsanzhar.ENThinker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class EmbeddingService {

    private final WebClient geminiClient;

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.embedModel}")
    private String embedModel;

    @Autowired
    public EmbeddingService(@Qualifier("geminiClient") WebClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public Mono<float[]> embed(String text) {

        var requestBody = new EmbedRequest(
                "models/" + embedModel,
                new Content( new Part[]{ new Part(text) } )
        );

        return geminiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:embedContent")
                        .queryParam("key", apiKey)
                        .build(embedModel))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .map(resp -> {
                    double[] d = resp.embedding().values();
                    float[] f = new float[d.length];
                    for (int i = 0; i < d.length; i++) f[i] = (float) d[i];
                    return f;
                });
    }

    record EmbedRequest(String model, Content content) {}
    record Content(Part[] parts) {}
    record Part(String text) {}
    record EmbedResponse(Embedding embedding) {}
    record Embedding(double[] values) {}
}
