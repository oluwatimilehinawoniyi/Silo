package com.silo.reporting.service;

import com.silo.reporting.dto.DashboardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class DashboardBroadcastService {

    private static final String EVENT_NAME = "dashboard-update";

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(DashboardResponse dashboard) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(dashboard));
            } catch (IOException | IllegalStateException ex) {
                log.warn("Removing dead SSE emitter: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
