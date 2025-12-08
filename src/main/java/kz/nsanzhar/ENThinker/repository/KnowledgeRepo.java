package kz.nsanzhar.ENThinker.repository;

import kz.nsanzhar.ENThinker.entity.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRepo extends JpaRepository<KnowledgeEntry, Long> {
}
