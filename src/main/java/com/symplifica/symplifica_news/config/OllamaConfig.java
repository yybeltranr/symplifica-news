package com.symplifica.symplifica_news.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OllamaConfig {
  @Bean
  public ChatModel ollamaChatModel(
          @Value("${app.ollama.base-url}") String baseUrl,
          @Value("${app.ollama.model-name}") String modelName,
          @Value("${app.ollama.temperature}") double temperature,
          @Value("${app.ollama.timeout}") Duration timeout) {

      return OllamaChatModel.builder()
              .baseUrl(baseUrl)
              .modelName(modelName)
              .temperature(temperature)
              .timeout(timeout)
              .build();
  }
}
