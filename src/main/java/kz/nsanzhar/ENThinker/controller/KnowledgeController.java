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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

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
        return Mono.fromCallable(() -> {

            int total = 0;

            // 1) Миграция из knowledge_base
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
                    System.err.println("❌ Ошибка миграции записи knowledge_base id=" + entry.getId());
                }
            }

            total += kbCount;

            // 2) Миграция из ent_info
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
                    System.err.println("❌ Ошибка миграции записи ent_info id=" + info.getId());
                }
            }

            total += entCount;

            return "✅ Миграция завершена. " +
                    "knowledge_base: " + kbCount + " записей, " +
                    "ent_info: " + entCount + " записей, " +
                    "всего: " + total;
        });
    }

}
