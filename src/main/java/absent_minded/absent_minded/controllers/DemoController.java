package absent_minded.absent_minded.controllers;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import absent_minded.absent_minded.services.AuthService;
import dev.langchain4j.model.openai.OpenAiChatModel;

@RestController
@RequestMapping("/api")
public class DemoController {
    private OpenAiChatModel model;
    private final AuthService auth;
    public DemoController(AuthService auth) {
        this.auth = auth;

        this.model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
    }
    @GetMapping("/demo")
    public String demo() {
        return "hello absent minded";
    }

    @PostMapping("/gpt")
    public String createTaskFromChat(@RequestHeader("Authorization") String authHeader,
                                     @RequestBody Map<String, String> body) throws IOException {

        String email = auth.emailFromAuthHeader(authHeader);

        String userInput = body.getOrDefault("message", "");
        String systemPrompt = """
            你現在是一個任務自動規劃助理，根據用戶簡短描述（可能只有一句話），自動補全並回傳符合以下格式的 JSON 物件，每個欄位都必須填寫合理的內容，不能留空或 null。
            若有欄位缺乏資訊，請你根據情境自行預設填寫。
                        
            請**只回傳 JSON 物件**，不要有任何解釋或多餘文字。
                        
            資料格式如下：
            {
                "label": "一句話說明此任務主題",
                "description": "請補全一段具體可執行的細節規劃或建議步驟"
            }
            """;

        String fullPrompt = systemPrompt + "\nUser(" + email + "): " + userInput;
        return model.chat(fullPrompt);
    }
}
