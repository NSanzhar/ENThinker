package kz.nsanzhar.ENThinker.service;

import kz.nsanzhar.ENThinker.dto.VectorSearchResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorService {

    private final WebClient qdrantClient;

    @Value("${qdrant.collection}")
    private String collection;

    /**
     * UPSERT with subject
     */
    public Mono<Void> upsert(String id, float[] vector, String text, String subject) {
        var payload = new HashMap<String, Object>();
        payload.put("text", text);
        payload.put("subject", subject);

        var point = new HashMap<String, Object>();
        point.put("id", UUID.randomUUID().toString());
        point.put("vector", vector);
        point.put("payload", payload);

        var body = Map.of("points", List.of(point));

        return qdrantClient.put()
                .uri("/collections/" + collection + "/points")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * SEARCH
     */
    public Mono<List<VectorSearchResult>> search(float[] vector, int topK) {
        var body = Map.of(
                "vector", vector,
                "limit", topK,
                "with_payload", true
        );

        return qdrantClient.post()
                .uri("/collections/" + collection + "/points/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .map(resp -> resp.result);
    }

    @Data
    static class SearchResponse {
        public List<VectorSearchResult> result;
    }
}
