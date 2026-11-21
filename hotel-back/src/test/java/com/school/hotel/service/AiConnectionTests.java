package com.school.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.hotel.dto.AiConnectionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AiConnectionTests {
    @TempDir Path dir;
    @Test void migratesOldSettingsAndKeepsConnectionsIndependent() throws Exception {
        var json = new ObjectMapper(); var file = dir.resolve("ai.json");
        json.writeValue(file.toFile(), new AiSettingsService.Settings(true,"https://one.example/v1/chat/completions","first","secret-one"));
        var service = new AiSettingsService(json,file.toString());
        assertEquals(1, service.connections().size());
        var request = new AiConnectionRequest();
        request.setName("Second"); request.setBaseUrl("https://two.example/v1/");
        request.setModel("second"); request.setApiKey("secret-two"); request.setEnabled(true);
        var second = service.saveConnection(null,request);
        var id = (String) second.get("id");
        assertEquals("first", service.current().model());
        service.activate(id);
        assertEquals("https://two.example/v1/chat/completions", service.current().endpoint());
        assertEquals("secret-two",service.current().apiKey());
        assertThrows(RuntimeException.class,()->service.deleteConnection(id));
        var restarted = new AiSettingsService(json,file.toString());
        assertEquals(2,restarted.connections().size());
        assertEquals("second",restarted.current().model());
        assertFalse(json.writeValueAsString(restarted.connections()).contains("secret-two"));
        request.setBaseUrl("https://other.example/v1"); request.setApiKey("");
        assertThrows(RuntimeException.class,()->restarted.saveConnection(id,request));
    }
}
