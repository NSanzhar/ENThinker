package kz.nsanzhar.ENThinker.service;

import kz.nsanzhar.ENThinker.dto.*;
import kz.nsanzhar.ENThinker.entity.QuestionEntity;
import kz.nsanzhar.ENThinker.repository.QuestionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuestionRepo questionRepo;
    private final Map<String, List<QuestionEntity>> activeSessions = new ConcurrentHashMap<>();

    public QuizStartResponse startQuiz(QuizStartRequest request) {
        List<QuestionEntity> questions = questionRepo.findRandomBySubject(
                request.getSubject(),
                request.getQuestionCount()
        );

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found for subject: " + request.getSubject());
        }

        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, questions);

        List<QuizQuestion> quizQuestions = questions.stream()
                .map(q -> new QuizQuestion(
                        questions.indexOf(q) + 1,
                        q.getId(),
                        q.getQuestion(),
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD()
                ))
                .collect(Collectors.toList());

        return new QuizStartResponse(
                sessionId,
                request.getSubject(),
                questions.size(),
                quizQuestions
        );
    }

    public QuizResult checkAnswers(QuizAnswerRequest request) {
        List<QuestionEntity> questions = activeSessions.get(request.getSessionId());

        if (questions == null) {
            throw new RuntimeException("Session not found or expired: " + request.getSessionId());
        }

        int correctCount = 0;
        List<QuizResult.QuestionResult> details = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            int questionNumber = i + 1;
            QuestionEntity question = questions.get(i);
            String userAnswerLetter = request.getAnswers().get(questionNumber);

            String correctAnswerText = question.getAnswer();

            String userAnswerText = getOptionText(question, userAnswerLetter);

            boolean isCorrect = correctAnswerText != null && correctAnswerText.equals(userAnswerText);
            if (isCorrect) correctCount++;

            String correctAnswerLetter = getCorrectLetter(question);

            details.add(new QuizResult.QuestionResult(
                    questionNumber,
                    question.getQuestion(),
                    userAnswerLetter != null ? userAnswerLetter : "Не отвечено",
                    correctAnswerLetter,
                    isCorrect
            ));
        }

        int score = (int) ((correctCount * 100.0) / questions.size());
        activeSessions.remove(request.getSessionId());

        return new QuizResult(questions.size(), correctCount, score, details);
    }

    private String getOptionText(QuestionEntity question, String letter) {
        if (letter == null) return null;

        switch (letter.toUpperCase()) {
            case "A": return question.getOptionA();
            case "B": return question.getOptionB();
            case "C": return question.getOptionC();
            case "D": return question.getOptionD();
            default: return null;
        }
    }

    private String getCorrectLetter(QuestionEntity question) {
        String correctText = question.getAnswer();

        if (correctText == null) return "?";

        if (correctText.equals(question.getOptionA())) return "A";
        if (correctText.equals(question.getOptionB())) return "B";
        if (correctText.equals(question.getOptionC())) return "C";
        if (correctText.equals(question.getOptionD())) return "D";

        return "?";
    }
}
