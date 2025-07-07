package absent_minded.absent_minded.controllers;

import java.io.IOException;
import java.util.Map;

import absent_minded.absent_minded.services.AgentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/demo")
    public String demo() {
        return "hello absent minded";
    }
    
    @PostMapping("/gpt")
    public ResponseEntity<String> createSimpleTask(
            @RequestHeader("Authorization") String header,
            @RequestBody Map<String, String> body
    ) throws IOException {
        String result = agentService.createSimpleTask(header, body);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}
