package com.pingpong.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class RealtimeEventPublisher {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RealtimeUpdateHandler realtimeUpdateHandler;

    public void publish(String type) {
        publish(type, Collections.emptyMap());
    }

    public void publish(String type, Object payload) {
        Map<String, Object> payloadMap = payload == null
            ? Collections.emptyMap()
            : objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() { });
        realtimeUpdateHandler.broadcast(type, payloadMap);
    }
}
