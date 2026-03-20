package absent_minded.absent_minded.dev;

import absent_minded.absent_minded.services.AgentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("dev")   // 只在 dev profile 下啟動
public class DevRunner implements CommandLineRunner {

    private final AgentService agentService;

    public DevRunner(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) throws Exception {

        // 寫死
        String projectId = "23ea7b6a-dedf-43f5-a5b5-94e088a0f9e1";

        // 你要測的「新任務內容」（刻意塞 RAG/metrics/log 讓它好命中）
        String label = "建立WBS分解";

        String description =
                "請插入到項目管理流程的規劃階段。";

        System.out.println("\n==============================");
        System.out.println("DEV TEST: suggestTaskLocationByAi (AI decides parent/depth)");
        System.out.println("==============================");

        AgentService.AgentResponse aiOnly =
                agentService.suggestTaskLocationByAi("DEV", projectId, label, description);

        System.out.println("[AI-ONLY] parentId=" + aiOnly.parentId());
        System.out.println("[AI-ONLY] depth=" + aiOnly.depth());
        System.out.println("[AI-ONLY] confidence=" + aiOnly.confidence());
        System.out.println("[AI-ONLY] explanation/reason=\n" + aiOnly.explanation());

        System.out.println("\n==============================");
        System.out.println("DEV TEST: suggestTaskLocation (HierarchyService decides + explainWithRag)");
        System.out.println("==============================");

        AgentService.AgentResponse hybrid =
                agentService.suggestTaskLocation("DEV", projectId, label, description);

        System.out.println("[HYBRID] parentId=" + hybrid.parentId());
        System.out.println("[HYBRID] depth=" + hybrid.depth());
        System.out.println("[HYBRID] confidence=" + hybrid.confidence());
        System.out.println("[HYBRID] explanation(from explainWithRag)=\n" + hybrid.explanation());

        System.out.println("\n===== DEV DONE =====\n");
    }

}
