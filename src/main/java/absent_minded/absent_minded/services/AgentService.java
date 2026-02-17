package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.TaskRepository;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.models.TaskData;

import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final OpenAiChatModel model;
    private final EmbeddingModel embeddingModel;
    private final AuthService auth;
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final HierarchyService hierrachyService;
    private final TokenCountEstimator tokenizer;

    public AgentService(
            AuthService auth,
            UserService userService,
            TaskRepository taskRepository,
            HierarchyService hierrachyService
    ) {
        this.auth = auth;
        this.userService = userService;
        this.taskRepository = taskRepository;
        this.hierrachyService = hierrachyService;

        // Chat 模型：用 langchain4j 的 demo server（不用 API key）
        this.model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
        this.tokenizer = new OpenAiTokenCountEstimator("gpt-4o-mini");
        // Embedding 模型：本地 BGE small zh v1.5 量化版（不用 API key）
        this.embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
    }

    public String getSimpleResponse(String prompt) {
        return model.chat(prompt);
    }
    private record ChatMetricsResult(String text, int inputTokens, int outputTokens, long latencyMs) {}

    private ChatMetricsResult chatWithMetrics(String prompt, String tag) {
        int inputTokens = tokenizer.estimateTokenCountInText(prompt);

        long t0 = System.nanoTime();
        String text = model.chat(prompt);
        long t1 = System.nanoTime();

        long ms = (t1 - t0) / 1_000_000;
        int outputTokens = text != null ? tokenizer.estimateTokenCountInText(text) : 0;

        log.info("[LLM] {} latency={} ms, input={} output={} total={}",
                tag, ms, inputTokens, outputTokens, inputTokens + outputTokens);

        return new ChatMetricsResult(text, inputTokens, outputTokens, ms);
    }
    // 改這個方法：不帶歷史資料的簡單版本，讓你可以先測試基本的 LLM 整合，再慢慢加上 RAG
    public String createSimpleTask(String header, Map<String, String> body) {
        String email = auth.emailFromAuthHeader(header);
        String userInput = body.get("message");
        if (userInput == null || userInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid prompt");
        }
        String systemPrompt = """
            你現在是一個任務自動規劃助理，根據用戶簡短描述（可能只有一句話），自動補全並回傳符合以下格式的 JSON 物件，每個欄位都必須填寫合理的內容，不能留空或 null。
            若有欄位缺乏資訊，請你根據情境自行預設填寫。

            請只回傳 JSON 物件，不要有任何解釋或多餘文字。

            資料格式如下：
            {
                "label": "一句話說明此任務主題",
                "description": "請補全一段具體可執行的細節規劃或建議步驟"
            }
            """;
        String prompt = systemPrompt + "\nUser(" + email + "): " + userInput;
//        String response = model.chat(prompt);
        ChatMetricsResult result = chatWithMetrics(prompt, "createSimpleTask");
        String response = result.text();
        if (response != null && !response.isBlank()) {
            // userService.addTokenUsage(header, userInput);
        }
        return response;
    }


    private String buildFinalPrompt(String email, String historyContext, String userInput) {

        String systemPrompt = """
                你現在是一個任務自動規劃助理（擁有多年管理及 AI 規劃經驗），負責在任務管理平台中自動補齊與延伸任務。
                
                 【Context（情境）】
                 這個平台以「任務樹（Task Tree）」形式呈現所有任務，每個任務之間存在層級與語意上的關聯。
                 你的工作是：根據使用者的一段簡短輸入，以及現有的所有任務資料，判斷是否需要補充新的任務，並輸出完整的任務定義。
                
                 【Role（角色）】
                 你是一名專業的任務自動規劃助理，擅長根據既有任務的層級與關聯性，推導出合理、具體且可執行的後續任務。
                 你會延續使用者既有的任務架構風格與寫法，生成務實且可落地的規劃。
                
                 【Instruction（指令）】
                 你必須：
                 1. 優先參考「歷史任務」中的寫法、風格、拆解方式。
                 2. 讓新的任務在內容上「延續或協助完成」既有任務，而不是完全無關。
                 3. 所有欄位皆須填寫合理內容，不能留空或使用 null。
                 4. 若缺乏資訊，請根據情境合理假設，並產出務實可執行的內容。
                 5. 若已有重複或語意相同任務，請勿重複生成。
                 6. 新任務需與至少一項現有任務具備明確關聯（作為子任務或父任務）。
                 7. 一次僅產生一個新任務。
                
                 【Purpose（目的）】
                 協助使用者自動補齊任務，使整體任務規劃更完整、清晰、可執行。
                
                 【Expectation（期望輸出）】
                 請**只回傳 JSON 物件**，不要附帶任何解釋或其他文字。
                 格式如下：
                 {
                     "label": "一句話說明此任務主題",
                     "description": "請補全一段具體可執行的細節規劃或建議步驟"
                 }
            """;

        String finalPrompt = systemPrompt
                + "\n\n=== 以下是使用者 " + email + " 的歷史任務（檢索後的最相關幾筆） ===\n"
                + historyContext
                + "\n=== 結束 ===\n"
                + "現在請根據以上歷史任務 + 使用者這次的需求，產生一個新的任務 JSON。\n"
                + "User(" + email + "): " + userInput;
        return finalPrompt;
    }


    // ✅ 有 RAG 的版本：會用使用者歷史 Task 做檢索
    public String createTaskWithHistoryRag(String header, Map<String, String> body) {
        String email = auth.emailFromAuthHeader(header);
        String userInput = body.get("message");

        if (userInput == null || userInput.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid prompt");
        }

        // 1. 取出這個使用者所有 task
        List<Task> tasks = taskRepository.findAllByOwnerId(email);
        log.info("[RAG] User {} has {} historical tasks", email, tasks.size());

        if (tasks.isEmpty()) {
            log.info("[RAG] No historical tasks found for user {}, fallback to simpleTask", email);
            return createSimpleTask(header, body);
        }

        // 2. 把 Task 轉成 TextSegment 並塞進 in-memory embedding store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        List<TextSegment> segments = toTaskSegments(tasks);

        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        // 3. 建 retriever，根據 userInput 做向量檢索
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.3)   // 想看更多結果可以先改成 0.0
                .build();

        Query query = Query.from(userInput);
        List<Content> relatedContents = retriever.retrieve(query);

        log.info("[RAG] Retrieved {} related contents for user {}", relatedContents.size(), email);

        StringBuilder contextBuilder = new StringBuilder();
        List<String> debugContexts = new ArrayList<>();

        int index = 1;

        for (Content content : relatedContents) {
            // 先直接把 Content 本體印出來，確認它實際型別跟內容
            log.info(
                    "[RAG] Raw retrieved content #{} | class = {} | value = {}",
                    index,
                    content.getClass().getName(),
                    content
            );

            // 如果真的剛好是 TextSegment，就再拿 text() 來用
            if (content instanceof TextSegment ts) {
                String text = ts.text();

                // 給模型的 context
                contextBuilder.append(text).append("\n---\n");
                debugContexts.add(text);

                // Log 清楚顯示它是某一個任務
                log.info(
                        "\n[RAG] ===== Retrieved Task #{} (TextSegment) =====\n{}\n========================================\n",
                        index,
                        text
                );
            }

            index++;
        }


        // 在 log 裡印出前幾筆 context，方便你確認 RAG 有沒有在用歷史資料
        for (int i = 0; i < Math.min(debugContexts.size(), 3); i++) {
            log.info("[RAG] Context #{} for user {}:\n{}", i + 1, email, debugContexts.get(i));
        }


        String historyContext = contextBuilder.toString();

        String finalPrompt = buildFinalPrompt(email, historyContext, userInput);

//        String response = model.chat(finalPrompt);
        ChatMetricsResult result = chatWithMetrics(finalPrompt, "createTaskWithHistoryRag");
        String response = result.text();

        log.info("[RAG] Model output for user {}: {}", email, response);
        log.info("token:{},time:{} ms", result.inputTokens() + result.outputTokens(), result.latencyMs());

        return response;
    }

    private String explainWithRag(String query, HierarchyService.Suggestion sug) {
        String evidenceParents = toEvidenceText("同層最相關候選（TopK parents）", sug.parentTopK());
        String evidenceChildren = toEvidenceText("下一層最相關候選（TopK children）", sug.childTopK());

        String systemPrompt = """
                你是一個任務樹狀拆解助理。
                系統已經決定「建議放置位置」（parentId / depth / confidence），你只能根據 evidence 解釋原因。
                你絕對不能更改 parentId 或 depth。
                如果 evidence 不足，你要明說「證據不足」並提出需要補充的資訊。
                輸出請用繁體中文，條列清楚，避免廢話。
            """;

        String userPrompt = """
                使用者想新增任務：
                %s
                
                系統建議放置位置：
                - parentId: %s
                - depth: %d
                - confidence(ratio): %.3f
                
                以下是檢索到的相關任務（evidence）：
                %s
                %s
                
                請回答：
                1) 建議放在此 parent 的理由（請引用 evidence 裡的任務 label/內容）
                2) 與哪些既有任務最相近（列出 2~5 筆）
                3) 若使用者其實想做的是別的方向，可能的替代放置點（1~2 個）
            """.formatted(query, sug.parentId(), sug.depth(), sug.ratio(), evidenceParents, evidenceChildren);

        String prompt = systemPrompt + "\n\n" + userPrompt;

        long t0 = System.nanoTime();
//        String response = model.chat(prompt);
        ChatMetricsResult result = chatWithMetrics(prompt, "explainWithRag");
        String response = result.text();
        long t1 = System.nanoTime();

        long ms = (t1 - t0) / 1_000_000;
        log.info("[LLM] explainWithRag chat() took {} ms", ms);
        log.info("token:{},time:{} ms", result.inputTokens() + result.outputTokens(), result.latencyMs());
        return response;
    }

    private String toEvidenceText(String title, List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "【" + title + "】(無)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(title).append("】\n");
        int i = 1;
        for (Task t : tasks) {
            String label = (t.getData() != null && t.getData().getLabel() != null) ? t.getData().getLabel() : "";
            String desc  = (t.getData() != null && t.getData().getDescription() != null) ? t.getData().getDescription() : "";
            sb.append(i++).append(". ")
                    .append("id=").append(safe(t.getId()))
                    .append(" parent=").append(safe(t.getParent()))
                    .append(" label=").append(label)
                    .append(" desc=").append(desc)
                    .append("\n");
        }
        return sb.toString();
    }


    public record AgentResponse(
            String parentId,
            int depth,
            double confidence,
            String explanation
    ) {}

    public record DualSuggestResponse(
            AgentResponse aiOnly,
            AgentResponse hybrid
    ) {}

    public DualSuggestResponse suggestBoth(
            String header,
            String projectId,
            String label,
            String description
    ) {
        AgentResponse aiOnly = suggestTaskLocationByAi(header, projectId, label, description);
        AgentResponse hybrid = suggestTaskLocation(header, projectId, label, description);

        return new DualSuggestResponse(aiOnly, hybrid);
    }


    public AgentResponse suggestTaskLocationByAi(
            String header,
            String projectId,
            String label,
            String description) {

        String query = (label == null ? "" : label) + " " + (description == null ? "" : description);

        // 抓專案任務清單給 LLM
        List<Task> allTasks = taskRepository.findAllByProject(projectId);
        String taskContext = allTasks.stream()
                .limit(15)
                .map(t -> String.format("%s: %s (%s)",
                        safe(t.getId()), safe(t.getData().getLabel()), safe(t.getParent())))
                .collect(Collectors.joining("\n"));

        String prompt = """
        你是任務樹自動規劃專家，負責分析任務清單並決定新任務的最佳放置位置。
        
        【規則】
        1. 仔細閱讀任務清單，找出與新任務語意最相近的任務
        2. 考慮任務樹的層級邏輯：新任務應該是某任務的子任務，或平行任務
        3. parentId 選最合適的那個任務ID，若完全不相關則用 null
        4. **reason 要詳細解釋**：為什麼選這個位置、與哪些任務相關、樹狀結構考量
        5. 只回傳 JSON，不要任何其他文字！
        
        【輸出格式】
        {
          "parentId": "任務ID或null",
          "depth": 建議深度數字,
          "confidence": 0.0-1.0,
          "reason": "詳細說明你的決策過程，至少 3 句話，包含相似任務分析與樹狀邏輯"
        }
        
        【任務清單】（格式：ID: 標題 (parentID)）
        %s
        
        【新任務需求】
        %s
        
        請根據任務清單結構與新任務內容，給出完整建議。
        """.formatted(taskContext, query);


        ChatMetricsResult result = chatWithMetrics(prompt, "suggestTaskLocationByAi");

        // 簡單解析（生產用 JSON lib）
        String parentId = extractJsonField(result.text(), "parentId");
        int depth = extractJsonIntField(result.text(), "depth");
        double confidence = extractJsonDoubleField(result.text(), "confidence");
        String reason = extractJsonField(result.text(), "reason");

        return new AgentResponse(parentId, depth, confidence, reason != null ? reason : "AI建議");
    }

    private String extractJsonField(String json, String field) {
        if (json == null) return null;
        String pattern = "\"" + field + "\":\\s*\"([^\"]+)\"";
        var matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int extractJsonIntField(String json, String field) {
        String val = extractJsonField(json, field);
        return val != null ? Integer.parseInt(val) : 0;
    }

    private double extractJsonDoubleField(String json, String field) {
        String val = extractJsonField(json, field);
        return val != null ? Double.parseDouble(val) : 0.5;
    }



    public AgentResponse suggestTaskLocation(
            String header,
            String projectId,
            String label,
            String description) {

        String query = (label == null ? "" : label) + " " + (description == null ? "" : description);

        // 1) 先用「樹收斂演算法」決策（不讓 LLM 決定）
        HierarchyService.Suggestion sug =
                hierrachyService.suggestParent(header, projectId, query, 5,5, 0.7, 10);

        log.info("[SUG] parentTopK size={}, childTopK size={}, ratio={}, parentId={}, depth={}",
                sug.parentTopK() == null ? -1 : sug.parentTopK().size(),
                sug.childTopK() == null ? -1 : sug.childTopK().size(),
                sug.ratio(), sug.parentId(), sug.depth());


        // 2) 再用 LLM 根據 evidence 解釋（RAG 的 G）
        String explanation = explainWithRag(query, sug);

        return new AgentResponse(sug.parentId(), sug.depth(), sug.ratio(), explanation);
    }



    // 把 Task 轉成 TextSegment，給 embedding 用
    private List<TextSegment> toTaskSegments(List<Task> tasks) {
        return tasks.stream()
                .map(task -> {
                    TaskData data = task.getData();
                    StringBuilder sb = new StringBuilder();
                    sb.append("TaskId: ").append(safe(task.getId())).append("\n");
                    sb.append("Project: ").append(safe(task.getProject())).append("\n");
                    sb.append("Status: ").append(safe(task.getStatus())).append("\n");

                    if (data != null) {
                        sb.append("Label: ").append(safe(data.getLabel())).append("\n");
                        sb.append("Description: ").append(safe(data.getDescription())).append("\n");
                        sb.append("Url: ").append(safe(data.getUrl())).append("\n");
                        if (data.getAssignees() != null && !data.getAssignees().isEmpty()) {
                            sb.append("Assignees: ")
                                    .append(String.join(",", data.getAssignees()))
                                    .append("\n");
                        }
                    }

                    return TextSegment.from(sb.toString());
                })
                .toList();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
