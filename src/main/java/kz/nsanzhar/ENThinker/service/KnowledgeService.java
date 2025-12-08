package kz.nsanzhar.ENThinker.service;

import kz.nsanzhar.ENThinker.dto.IngestRequest;
import kz.nsanzhar.ENThinker.dto.AskRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final RagService ragService;


    /**
     * INGEST
     */
    public Mono<Void> ingest(IngestRequest req) {

        String subject = (req.getSubject() == null || req.getSubject().isBlank())
                ? "general"
                : req.getSubject();

        var chunks = chunkingService.chunkText(req.getText(), 200);

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


    /**
     * ASK
     */
    public Mono<String> ask(AskRequest req) {
        String question = req.getQuestion();

        return embeddingService.embed(question)
                .flatMap(vector -> vectorService.search(vector, 5))
                .flatMap(results ->
                        ragService.answerWithContext(question, results)
                );
    }
}
