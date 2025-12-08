package kz.nsanzhar.ENThinker.controller;

import kz.nsanzhar.ENThinker.dto.AskRequest;
import kz.nsanzhar.ENThinker.dto.IngestRequest;
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
    private final KnowledgeRepo knowledgeRepo; // Добавлено

    @PostMapping("/ingest")
    public Mono<String> ingest(@RequestBody IngestRequest request) {
        return knowledgeService.ingest(request)
                .then(Mono.just("Document successfully ingested into vector DB."));
    }

    @PostMapping("/ask")
    public Mono<String> ask(@RequestBody AskRequest request) {
        return knowledgeService.ask(request);
    }

    @PostMapping("/migrate")
    public Mono<String> migrateFromDatabase() {
        return Mono.fromCallable(() -> {
            // Получаем все записи из MySQL
            List<KnowledgeEntry> entries = knowledgeRepo.findAll();

            int count = 0;
            for (KnowledgeEntry entry : entries) {
                try {
                    // Отправляем каждую запись через ingest
                    IngestRequest request = new IngestRequest();
                    request.setText(entry.getContent()); // Используем getText() вместо getContent()
                    request.setSubject(entry.getSubject()); // Используем getSubject() вместо getTitle()

                    knowledgeService.ingest(request).block();
                    count++;
                } catch (Exception e) {
                    System.err.println("Ошибка миграции записи: " + entry.getId());
                }
            }

            return "Мигрировано записей: " + count;
        });
    }
}
