package kz.nsanzhar.ENThinker.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {
    private int estimateTokens(String text) {
        return text.length() / 4;
    }

    public List<String> chunkText(String text, int maxTokens) {
        String[] sentences = text.split("(?<=[.!?])\\s+");

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {

            int sentenceTokens = estimateTokens(sentence);

            // если предложение больше лимита режем грубо
            if (sentenceTokens > maxTokens) {
                chunks.add(sentence.trim());
                continue;
            }

            // если чанк переполнится начинаем новый
            if (currentTokens + sentenceTokens > maxTokens) {
                chunks.add(currentChunk.toString().trim());
                currentChunk.setLength(0);
                currentTokens = 0;
            }

            // добавляем предложение в текущий чанк
            currentChunk.append(sentence).append(" ");
            currentTokens += sentenceTokens;
        }

        // добавляем последний чанк
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
