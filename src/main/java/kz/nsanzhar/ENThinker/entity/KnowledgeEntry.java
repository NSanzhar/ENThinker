package kz.nsanzhar.ENThinker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "knowledge_base")
@Getter
@Setter
public class KnowledgeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String topic;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String source;
    private String tags;
    private LocalDate updatedAt;
}
