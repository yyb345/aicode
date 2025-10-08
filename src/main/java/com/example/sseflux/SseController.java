package com.example.sseflux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalTime;

@RestController
public class SseController {

    @GetMapping(path = "/sse-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sseStream() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> "Event " + sequence + " at " + LocalTime.now());
    }
}
