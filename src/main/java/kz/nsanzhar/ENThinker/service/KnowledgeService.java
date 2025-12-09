package kz.nsanzhar.ENThinker.service;

import kz.nsanzhar.ENThinker.dto.IngestRequest;
import kz.nsanzhar.ENThinker.dto.AskRequest;
import kz.nsanzhar.ENThinker.dto.RagResponse;
import kz.nsanzhar.ENThinker.dto.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

        log.info("Начало загрузки документа. Subject: {}, длина текста: {}", subject, req.getText().length());

        var chunks = chunkingService.chunkText(req.getText(), maxTokens);
        log.debug("Текст разделён на {} чанков", chunks.size());

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
                ).doOnSuccess(v -> log.info("Документ успешно загружен. Subject: {}", subject))
                .doOnError(e -> log.error("Ошибка загрузки документа: {}", e.getMessage(), e));
    }

    public Mono<RagResponse> ask(AskRequest req) {
        String question = req.getQuestion();
        int topK = (req.getTopK() > 0) ? req.getTopK() : 5;

        double threshold = (req.getMinScore() != null && req.getMinScore() > 0)
                ? req.getMinScore()
                : scoreThreshold;

        log.info("Поиск ответа на вопрос. TopK: {}, Threshold: {}", topK, threshold);
        log.debug("Вопрос: {}", question);

        return embeddingService.embed(question)
                .flatMap(vector -> vectorService.search(vector, topK))
                .flatMap(results -> {
                    List<VectorSearchResult> filteredResults = results.stream()
                            .filter(r -> r.getScore() >= threshold)
                            .collect(Collectors.toList());

                    log.debug("Найдено {} результатов, после фильтрации: {}", results.size(), filteredResults.size());

                    if (filteredResults.isEmpty()) {
                        log.warn("Не найдено релевантных результатов для вопроса");
                        return Mono.just(new RagResponse(
                                "❌ В моей базе знаний недостаточно информации, чтобы полноценно ответить на этот вопрос. " +
                                        "Найдено " + results.size() + " результатов, но все имеют низкую релевантность (< " +
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
                        log.warn("Контекст пуст после обработки источников");
                        return Mono.just(new RagResponse(
                                "❌ В моей базе знаний недостаточно информации, чтобы полноценно ответить на этот вопрос.",
                                sources,
                                results.size()
                        ));
                    }

                    return geminiPromptService.askGemini(question, context.toString())
                            .map(answer -> {
                                log.info("Ответ сгенерирован успешно");
                                return new RagResponse(answer, sources, results.size());
                            });
                })
                .onErrorResume(e -> {
                    log.error("Ошибка обработки вопроса: {}", e.getMessage(), e);
                    return Mono.just(new RagResponse(
                            "❌ Произошла ошибка при обработке вопроса: " + e.getMessage(),
                            List.of(),
                            0
                    ));
                });
    }
}
