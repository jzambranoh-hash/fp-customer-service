package com.nttdata.customer_service.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/customers")
public class PingController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/ping")
    public Mono<String> ping() {
        return Mono.just("Customer Service is running on port: " + port);
    }

}
