package kz.nsanzhar.ENThinker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ent_info")
@Getter
@Setter
public class EntInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;          // ← ОБЯЗАТЕЛЬНОЕ поле‑идентификатор

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String content;
}
