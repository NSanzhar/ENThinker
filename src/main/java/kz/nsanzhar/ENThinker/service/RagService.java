package kz.nsanzhar.ENThinker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final GeminiPromptService geminiPromptService;


    /**
     * INGEST — загрузка текста
     */
    public Mono<Void> ingestDocument(String text) {

        var chunks = chunkingService.chunkText(text, 120);

        return Mono.when(
                chunks.stream().map(chunk ->
                        embeddingService.embed(chunk)
                                .flatMap(vec -> vectorService.upsert(
                                        UUID.randomUUID().toString(),
                                        vec,
                                        chunk,
                                        "general"
                                ))
                ).toList()
        );
    }


    /**
     * ASK — собрать контекст и отправить в Gemini
     */
    public Mono<String> answerWithContext(String question, List<VectorService.SearchResult> results) {

        StringBuilder ctx = new StringBuilder();

        results.forEach(r -> {
            Object t = r.getPayload().get("text");
            if (t != null) ctx.append(t).append("\n");
        });

        return geminiPromptService.askGemini(question, ctx.toString());
    }


    /**
     * RAG ответ с полным объектом
     */
    public record RagAnswer(String answer, String usedContext) { }
}
