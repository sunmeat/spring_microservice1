package com.sunmeat.contactlist.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {
    @Bean
    @Primary // позначає цей ObjectMapper як основний для використання в додатку
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
