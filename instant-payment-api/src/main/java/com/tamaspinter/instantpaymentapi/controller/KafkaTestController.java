package com.tamaspinter.instantpaymentapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Kafka API", description = "Endpoints for sending test Kafka messages")
@RestController
@RequestMapping("/api/test-kafka")
public class KafkaTestController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Sends a test message to a Kafka topic.
     * @param message The message to be sent.
     * @return Confirmation of message sent.
     */
    @Operation(
            summary = "Send a test message to Kafka",
            description = "Sends a test message to the 'test-topic' Kafka topic and returns confirmation.",
            operationId = "sendTestMessage"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(
                                    name = "Example Kafka Message",
                                    value = "{\"message\":\"Hello, World!\"}"
                            )
                    )
            )
    })
    @GetMapping("/send")
    public String sendTestMessage(@RequestParam("msg") String message) {
        kafkaTemplate.send("test-topic", message);
        return "Message sent: " + message;
    }
}
