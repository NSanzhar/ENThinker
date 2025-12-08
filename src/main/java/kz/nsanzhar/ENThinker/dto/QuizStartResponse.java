package kz.nsanzhar.ENThinker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuizStartResponse {
    private String sessionId;
    private String subject;
    private int totalQuestions;
    private List<QuizQuestion> questions;
}
