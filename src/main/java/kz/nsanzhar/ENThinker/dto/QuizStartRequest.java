package kz.nsanzhar.ENThinker.dto;

import lombok.Data;

@Data
public class QuizStartRequest {
    private String subject;
    private int questionCount = 10;
}
