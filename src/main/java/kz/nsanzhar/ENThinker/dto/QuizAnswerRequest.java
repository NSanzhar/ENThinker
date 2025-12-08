package kz.nsanzhar.ENThinker.dto;

import lombok.Data;

import java.util.Map;

@Data
public class QuizAnswerRequest {
    private String sessionId;
    private Map<Integer, String> answers;
}
