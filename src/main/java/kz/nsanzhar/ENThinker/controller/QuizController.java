package kz.nsanzhar.ENThinker.controller;

import kz.nsanzhar.ENThinker.dto.*;
import kz.nsanzhar.ENThinker.dto.QuizAnswerRequest;
import kz.nsanzhar.ENThinker.dto.QuizResult;
import kz.nsanzhar.ENThinker.dto.QuizStartRequest;
import kz.nsanzhar.ENThinker.dto.QuizStartResponse;
import kz.nsanzhar.ENThinker.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/start")
    public QuizStartResponse startQuiz(@RequestBody QuizStartRequest request) {
        return quizService.startQuiz(request);
    }

    @PostMapping("/check")
    public QuizResult checkAnswers(@RequestBody QuizAnswerRequest request) {
        return quizService.checkAnswers(request);
    }
}
