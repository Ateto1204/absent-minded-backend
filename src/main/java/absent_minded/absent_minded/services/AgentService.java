package absent_minded.absent_minded.services;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AgentService {
    private OpenAiChatModel model;
    private final AuthService auth;
    private final UserService userService;

    public AgentService(AuthService auth, UserService userService) {
        this.auth = auth;
        this.userService = userService;

        this.model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
    }

    public String getSimpleResponse(String prompt) {
        return model.chat(prompt);
    }

    public String createSimpleTask(String header, Map<String, String> body) {
        String email = auth.emailFromAuthHeader(header);
        String userInput = body.get("message");
        if (userInput == null || userInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid prompt");
        }
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
        String prompt = systemPrompt + "\nUser(" + email + "): " + userInput;
        String response = model.chat(prompt);
        if (response != null && !response.isBlank()) {
//            userService.addTokenUsage(header, userInput);
        }
        return response;
    }
}
