package kz.nsanzhar.ENThinker.service;

import kz.nsanzhar.ENThinker.dto.IngestRequest;
import kz.nsanzhar.ENThinker.dto.AskRequest;
import kz.nsanzhar.ENThinker.dto.RagResponse;
import kz.nsanzhar.ENThinker.dto.VectorSearchResult; // ← ОБНОВИТЬ ИМПОРТ
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final GeminiPromptService geminiPromptService;

    @Value("${chunking.max-tokens:200}")
    private int maxTokens;

    @Value("${rag.score-threshold:0.52}")
    private double scoreThreshold;

    public Mono<Void> ingest(IngestRequest req) {
        String subject = (req.getSubject() == null || req.getSubject().isBlank())
                ? "general"
                : req.getSubject();

        var chunks = chunkingService.chunkText(req.getText(), maxTokens);

        return Mono.when(
                chunks.stream()
                        .map(chunk -> embeddingService.embed(chunk)
                                .flatMap(embed ->
                                        vectorService.upsert(
                                                UUID.randomUUID().toString(),
                                                embed,
                                                chunk,
                                                subject
                                        )
                                ))
                        .toList()
        );
    }

    public Mono<RagResponse> ask(AskRequest req) {
        String question = req.getQuestion();
        int topK = (req.getTopK() > 0) ? req.getTopK() : 5;

        double threshold = (req.getMinScore() != null && req.getMinScore() > 0)
                ? req.getMinScore()
                : scoreThreshold;

        return embeddingService.embed(question)
                .flatMap(vector -> vectorService.search(vector, topK))
                .flatMap(results -> {
                    // Теперь тип VectorSearchResult вместо VectorService.SearchResult
                    List<VectorSearchResult> filteredResults = results.stream()
                            .filter(r -> r.getScore() >= threshold)
                            .collect(Collectors.toList());

                    if (filteredResults.isEmpty()) {
                        return Mono.just(new RagResponse(
                                "❌ I don't have enough information to answer this question. " +
                                        "The knowledge base doesn't contain relevant data (found " +
                                        results.size() + " results, but all had low relevance score < " +
                                        threshold + ").",
                                List.of(),
                                results.size()
                        ));
                    }

                    StringBuilder context = new StringBuilder();
                    List<RagResponse.SourceInfo> sources = filteredResults.stream()
                            .map(result -> {
                                Object text = result.getPayload().get("text");
                                Object subject = result.getPayload().get("subject");

                                if (text != null) {
                                    context.append(text).append("\n\n");
                                }

                                return new RagResponse.SourceInfo(
                                        text != null ? text.toString() : "N/A",
                                        subject != null ? subject.toString() : "general",
                                        result.getScore(),
                                        result.getId()
                                );
                            })
                            .collect(Collectors.toList());

                    if (context.length() == 0) {
                        return Mono.just(new RagResponse(
                                "❌ I don't have enough information to answer this question.",
                                sources,
                                results.size()
                        ));
                    }

                    return geminiPromptService.askGemini(question, context.toString())
                            .map(answer -> new RagResponse(
                                    answer,
                                    sources,
                                    results.size()
                            ));
                })
                .onErrorResume(e -> {
                    System.err.println("❌ Error during ask: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(new RagResponse(
                            "❌ Sorry, an error occurred while processing your question: " + e.getMessage(),
                            List.of(),
                            0
                    ));
                });
    }
}
