package com.tamaspinter.instantpaymentapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-kafka")
public class KafkaTestController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/send")
    public String sendTestMessage(@RequestParam("msg") String message) {
        kafkaTemplate.send("test-topic", message);
        return "Message sent: " + message;
    }
}
