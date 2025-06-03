package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.services.AuthService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DemoController.class)
public class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoController controller;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void setup() {
        // 模擬 AuthService 回傳 email
        when(authService.emailFromAuthHeader("Bearer test-token"))
                .thenReturn("test@example.com");

        // 建立 mock 的 OpenAiChatModel 並回傳固定 JSON
        OpenAiChatModel mockModel = Mockito.mock(OpenAiChatModel.class);
        when(mockModel.chat(anyString()))
                .thenReturn("""
                        {
                            "label": "測試任務",
                            "description": "這是一段描述"
                        }
                        """);

        // 用反射將 model 注入 controller
        ReflectionTestUtils.setField(controller, "model", mockModel);
    }

    @Test
    void testDemoEndpoint() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello absent minded"));
    }

    @Test
    void testGptEndpoint() throws Exception {
        String requestBody = """
                {
                    "message": "幫我安排一個閱讀計畫"
                }
                """;

        mockMvc.perform(post("/api/gpt")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "label": "測試任務",
                            "description": "這是一段描述"
                        }
                        """));
    }
}