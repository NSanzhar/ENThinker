package kz.nsanzhar.ENThinker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuizResult {
    private int totalQuestions;
    private int correctAnswers;
    private int score; // процент
    private List<QuestionResult> details;

    @Data
    @AllArgsConstructor
    public static class QuestionResult {
        private int number;
        private String question;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
    }
}
