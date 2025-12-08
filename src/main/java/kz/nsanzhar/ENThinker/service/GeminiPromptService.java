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
            Ты — интеллектуальная система, отвечающая ТОЛЬКО на русском языке.

            ПРАВИЛА:
            1. Используй ТОЛЬКО информацию из контекста ниже.
            2. Отвечай строго на русском языке, даже если вопрос на другом языке.
            3. Фокусируйся на наиболее релевантных частях контекста.
            4. Структурируй ответ: абзацы и маркированные списки (*).
            5. Если в контексте недостаточно информации, напиши: "В моей базе знаний недостаточно информации, чтобы полноценно ответить на этот вопрос."
            6. Будь конкретным и фактическим, не выдумывай.

            КОНТЕКСТ:
            %s

            ВОПРОС:
            %s

            ОТВЕТ НА РУССКОМ ЯЗЫКЕ:
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
                    return Mono.just("❌ Ошибка при обращении к модели ИИ. Попробуйте ещё раз.");
                });
    }

    record GeminiRequest(List<GeminiContent> contents) {}
    record GeminiContent(List<GeminiPart> parts) {}
    record GeminiPart(String text) {}
    record GeminiResponse(List<GeminiCandidate> candidates) {}
    record GeminiCandidate(GeminiGeneratedContent content) {}
    record GeminiGeneratedContent(List<GeminiPart> parts) {}
}
