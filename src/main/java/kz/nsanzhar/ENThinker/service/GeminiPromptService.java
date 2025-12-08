package kz.nsanzhar.ENThinker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GeminiPromptService {

    private final WebClient geminiClient;

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.chatModel}")
    private String chatModel;

    @Autowired
    public GeminiPromptService(@Qualifier("geminiClient") WebClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public Mono<String> askGemini(String question, String context) {

        String prompt = """
                You are an AI assistant. Use ONLY the provided context to answer the question.

                CONTEXT:
                %s

                QUESTION:
                %s

                If the answer is not in the context, say: "I don't know based on the provided knowledge."
                """.formatted(context, question);

        var requestBody = new GeminiRequest(
                List.of(
                        new GeminiContent(
                                List.of(new GeminiPart(prompt))
                        )
                )
        );

        return geminiClient.post()
                .uri("/v1/models/" + chatModel + ":generateContent?key=" + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(resp -> resp.candidates().get(0).content().parts().get(0).text());
    }

    record GeminiRequest(List<GeminiContent> contents) {}
    record GeminiContent(List<GeminiPart> parts) {}
    record GeminiPart(String text) {}
    record GeminiResponse(List<GeminiCandidate> candidates) {}
    record GeminiCandidate(GeminiGeneratedContent content) {}
    record GeminiGeneratedContent(List<GeminiPart> parts) {}
}
