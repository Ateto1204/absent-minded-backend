package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.services.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentService agentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testDemo() throws Exception {
        mockMvc.perform(get("/api/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello absent minded"));
    }

    @Test
    public void testCreateSimpleTask_success() throws Exception {
        String header = "Bearer demo-token";
        Map<String, String> body = Collections.singletonMap("message", "Test message");
        String fakeResponse = "{\"label\":\"Task\",\"description\":\"Details\"}";

        when(agentService.createSimpleTask(eq(header), eq(body)))
                .thenReturn(fakeResponse);

        mockMvc.perform(post("/api/gpt")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(fakeResponse));
    }

    @Test
    public void testCreateSimpleTask_badRequest() throws Exception {
        String header = "Bearer demo-token";
        Map<String, String> body = Collections.emptyMap();

        when(agentService.createSimpleTask(eq(header), anyMap()))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid prompt"));

        mockMvc.perform(post("/api/gpt")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Invalid prompt"));
    }

    @Test
    public void testCreateSimpleTask_unauthorized() throws Exception {
        String header = "Bearer invalid-token";
        Map<String, String> body = Collections.singletonMap("message", "Hi");

        when(agentService.createSimpleTask(eq(header), anyMap()))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header"));

        mockMvc.perform(post("/api/gpt")
                        .header("Authorization", header)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Missing or invalid Authorization header"));
    }
}
