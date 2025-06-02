package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class DemoControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private OpenAiChatModel model;
    private DemoController demoController;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        authService = Mockito.mock(AuthService.class);
        model = Mockito.mock(OpenAiChatModel.class);

        demoController = new DemoController(authService);
        Field modelField = DemoController.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(demoController, model);

        mockMvc = MockMvcBuilders.standaloneSetup(demoController).build();
    }

    @Test
    void testCreateTaskFromChat() throws Exception {
        // Arrange
        String mockEmail = "test@example.com";
        String userMessage = "幫我規劃明天的期末報告準備";
        String mockResponse = """
            {
              "label": "\\u671F\\u672B\\u5831\\u544A\\u6E96\\u5099",
              "description": "\\u6E96\\u5099\\u5831\\u544A\\u7684\\u8CC7\\u6599\\u3001\\u88FD\\u4F5C\\u7C21\\u5831\\uFF0C\\u4E26\\u5B89\\u6392\\u4E00\\u6B21\\u5F69\\u6392\\u3002"
            }
            """;

        Mockito.when(authService.emailFromAuthHeader("Bearer test-token")).thenReturn(mockEmail);
        Mockito.when(model.chat(anyString())).thenReturn(mockResponse);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> prettyJson = mapper.readValue(mockResponse, Map.class);

        System.out.println("========== Test Input ==========");
        System.out.println("{\"message\": \"" + userMessage + "\"}");

        System.out.println("\n====== Expected JSON (decoded) ======");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyJson));
        System.out.println("=====================================\n");

        // Act + Assert
        mockMvc.perform(post("/gpt")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"" + userMessage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("期末報告準備"))
                .andExpect(jsonPath("$.description").value("準備報告的資料、製作簡報，並安排一次彩排。"));
    }
}