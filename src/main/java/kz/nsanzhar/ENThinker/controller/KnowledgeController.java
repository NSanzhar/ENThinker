package kz.nsanzhar.ENThinker.controller;

import kz.nsanzhar.ENThinker.dto.AskRequest;
import kz.nsanzhar.ENThinker.dto.IngestRequest;
import kz.nsanzhar.ENThinker.dto.RagResponse;
import kz.nsanzhar.ENThinker.entity.EntInfo;
import kz.nsanzhar.ENThinker.entity.KnowledgeEntry;
import kz.nsanzhar.ENThinker.repository.EntInfoRepo;
import kz.nsanzhar.ENThinker.repository.KnowledgeRepo;
import kz.nsanzhar.ENThinker.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeRepo knowledgeRepo;
    private final EntInfoRepo entInfoRepo;

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
        log.info("Начало миграции данных из БД в векторное хранилище");

        return Mono.fromCallable(() -> {
            int total = 0;

            // 1) Миграция из knowledge_base
            log.info("Миграция таблицы knowledge_base...");
            List<KnowledgeEntry> knowledgeEntries = knowledgeRepo.findAll();
            int kbCount = 0;

            for (KnowledgeEntry entry : knowledgeEntries) {
                try {
                    if (entry.getContent() == null || entry.getContent().isBlank()) {
                        continue;
                    }

                    IngestRequest request = new IngestRequest();
                    request.setText(entry.getContent());
                    request.setSubject(
                            (entry.getSubject() == null || entry.getSubject().isBlank())
                                    ? "knowledge_base"
                                    : entry.getSubject()
                    );

                    knowledgeService.ingest(request).block();
                    kbCount++;
                } catch (Exception e) {
                    log.error("Ошибка миграции записи knowledge_base id={}: {}", entry.getId(), e.getMessage());
                }
            }

            total += kbCount;
            log.info("Миграция knowledge_base завершена: {} записей", kbCount);

            // 2) Миграция из ent_info
            log.info("Миграция таблицы ent_info...");
            List<EntInfo> entInfos = entInfoRepo.findAll();
            int entCount = 0;

            for (EntInfo info : entInfos) {
                try {
                    if (info.getContent() == null || info.getContent().isBlank()) {
                        continue;
                    }

                    IngestRequest request = new IngestRequest();
                    request.setText(info.getContent());
                    request.setSubject("ent_info");

                    knowledgeService.ingest(request).block();
                    entCount++;
                } catch (Exception e) {
                    log.error("Ошибка миграции записи ent_info id={}: {}", info.getId(), e.getMessage());
                }
            }

            total += entCount;
            log.info("Миграция ent_info завершена: {} записей", entCount);
            log.info("Миграция завершена успешно. Всего обработано: {} записей", total);

            return "✅ Миграция завершена. " +
                    "knowledge_base: " + kbCount + " записей, " +
                    "ent_info: " + entCount + " записей, " +
                    "всего: " + total;
        });
    }
}
