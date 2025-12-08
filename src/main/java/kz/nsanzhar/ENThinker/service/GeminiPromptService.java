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
        You are a knowledgeable AI assistant. Answer the question using ONLY the provided context.
        
        RULES:
        1. Use ONLY information from the context below
        2. Focus on the most relevant parts of the context
        3. Ignore unrelated information
        4. Structure your answer clearly with bullet points or paragraphs
        5. If the context doesn't contain enough information, say: "I don't have enough information in my knowledge base to fully answer this question."
        6. Be specific and cite facts from the context
        
        CONTEXT:
        %s
        
        QUESTION:
        %s
        
        ANSWER (be clear, structured, and factual):
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
                .map(resp -> resp.candidates().get(0).content().parts().get(0).text())
                .onErrorResume(e -> {
                    System.err.println("❌ Gemini API error: " + e.getMessage());
                    return Mono.just("❌ Error communicating with AI model. Please try again.");
                });
    }

    record GeminiRequest(List<GeminiContent> contents) {}
    record GeminiContent(List<GeminiPart> parts) {}
    record GeminiPart(String text) {}
    record GeminiResponse(List<GeminiCandidate> candidates) {}
    record GeminiCandidate(GeminiGeneratedContent content) {}
    record GeminiGeneratedContent(List<GeminiPart> parts) {}
}
