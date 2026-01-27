package absent_minded.absent_minded.services;

import absent_minded.absent_minded.repositories.TaskRepository;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.models.TaskData;

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

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final OpenAiChatModel model;
    private final EmbeddingModel embeddingModel;
    private final AuthService auth;
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final HierarchyService hierrachyService;


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

        // Embedding 模型：本地 BGE small zh v1.5 量化版（不用 API key）
        this.embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
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

            請只回傳 JSON 物件，不要有任何解釋或多餘文字。

            資料格式如下：
            {
                "label": "一句話說明此任務主題",
                "description": "請補全一段具體可執行的細節規劃或建議步驟"
            }
            """;
        String prompt = systemPrompt + "\nUser(" + email + "): " + userInput;
        String response = model.chat(prompt);
        if (response != null && !response.isBlank()) {
            // userService.addTokenUsage(header, userInput);
        }
        return response;
    }

    private String buildFinalPrompt(String email, String historyContext, String userInput) {

        String systemPrompt = """
            你現在是一個任務自動規劃助理，會先閱讀使用者以前建立的相關任務，再根據使用者這次的簡短描述，自動補全並回傳符合以下格式的 JSON 物件。

            你必須：
            1. 優先參考「歷史任務」中的寫法、風格、拆解方式
            2. 讓新的任務在內容上「延續 / 協助完成」既有任務，而不是完全無關
            3. 所有欄位都必須填寫合理內容，不能留空或 null
            4. 若缺乏資訊，請根據情境合理假設，但要保持務實可執行

            請只回傳 JSON 物件，不要有任何解釋或多餘文字。

            資料格式如下：
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

        String response = model.chat(finalPrompt);


        log.info("[RAG] Model output for user {}: {}", email, response);

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
        String response = model.chat(prompt);
        long t1 = System.nanoTime();

        long ms = (t1 - t0) / 1_000_000;
        log.info("[LLM] explainWithRag chat() took {} ms", ms);
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
