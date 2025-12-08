package kz.nsanzhar.ENThinker.controller;

import kz.nsanzhar.ENThinker.dto.AskRequest;
import kz.nsanzhar.ENThinker.dto.IngestRequest;
import kz.nsanzhar.ENThinker.dto.RagResponse;
import kz.nsanzhar.ENThinker.entity.KnowledgeEntry;
import kz.nsanzhar.ENThinker.repository.KnowledgeRepo;
import kz.nsanzhar.ENThinker.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeRepo knowledgeRepo;

    @PostMapping("/ingest")
    public Mono<String> ingest(@RequestBody IngestRequest request) {
        return knowledgeService.ingest(request)
                .then(Mono.just("✅ Document successfully ingested into vector DB."));
    }

    @PostMapping("/ask")
    public Mono<RagResponse> ask(@RequestBody AskRequest request) {
        return knowledgeService.ask(request);
    }

    @PostMapping("/migrate")
    public Mono<String> migrateFromDatabase() {
        return Mono.fromCallable(() -> {
            List<KnowledgeEntry> entries = knowledgeRepo.findAll();
            int count = 0;

            for (KnowledgeEntry entry : entries) {
                try {
                    IngestRequest request = new IngestRequest();
                    request.setText(entry.getContent());
                    request.setSubject(entry.getSubject());

                    knowledgeService.ingest(request).block();
                    count++;
                } catch (Exception e) {
                    System.err.println("❌ Ошибка миграции записи: " + entry.getId());
                }
            }

            return "✅ Мигрировано записей: " + count;
        });
    }
}
