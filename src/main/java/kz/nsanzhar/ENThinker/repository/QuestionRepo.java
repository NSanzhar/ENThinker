package kz.nsanzhar.ENThinker.repository;

import kz.nsanzhar.ENThinker.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepo extends JpaRepository<QuestionEntity, Long> {

    @Query(value = "SELECT * FROM questions WHERE subject = :subject ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuestionEntity> findRandomBySubject(@Param("subject") String subject, @Param("limit") int limit);
}
