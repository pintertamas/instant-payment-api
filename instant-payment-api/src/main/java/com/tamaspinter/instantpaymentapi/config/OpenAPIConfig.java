package com.tamaspinter.instantpaymentapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Instant Payment API")
                        .version("1.0")
                        .description("API for processing instant payments with balance checks and Kafka notifications"));
    }
}
