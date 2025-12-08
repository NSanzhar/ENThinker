package kz.nsanzhar.ENThinker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${gemini.apiKey}")
    private String geminiApiKey;

    @Bean("geminiClient")
    public WebClient geminiClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getApiKeyQuery() {
        return "?key=" + geminiApiKey;
    }
}
