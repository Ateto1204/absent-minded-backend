package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HierarchyService {

    private final TaskRepository taskRepo;
    private final ProjectService projectService;
    private final EmbeddingModel embeddingModel;

    // ✅ embedding cache
    private final Map<String, double[]> embeddingCache = new ConcurrentHashMap<>();

    public HierarchyService(TaskRepository taskRepo,
                            ProjectService projectService,
                            EmbeddingModel embeddingModel) {
        this.taskRepo = taskRepo;
        this.projectService = projectService;
        this.embeddingModel = embeddingModel;
    }

    public Suggestion suggestParent(String header,
                                    String projectId,
                                    String newTaskText,
                                    int k,
                                    int kPrime,
                                    double M,
                                    int maxDepth) {

        Project project = projectService.getProjectById(header, projectId);
        String rootId = project.getRootTask();
        if (rootId == null || rootId.isBlank()) {
            return Suggestion.placeUnder(null, 0, 0.0, List.of(), List.of());
        }

        List<Task> all = taskRepo.findAllByProject(projectId);

        Map<String, List<Task>> childrenMap = new HashMap<>();
        Map<String, Task> byId = new HashMap<>();
        for (Task t : all) {
            byId.put(t.getId(), t);
            if (t.getParent() != null) {
                childrenMap.computeIfAbsent(t.getParent(), __ -> new ArrayList<>()).add(t);
            }
        }

        if (!byId.containsKey(rootId)) {
            return Suggestion.placeUnder(null, 0, 0.0, List.of(), List.of());
        }

        Map<Integer, List<Task>> depthBuckets = buildDepthBuckets(childrenMap, rootId);

        // ✅ query embedding
        double[] qVec = embedToDoubleArray(newTaskText);

        int depth = 0;
        double lastRatio = 0.0;
        List<Task> lastParentTopK = List.of();
        List<Task> lastChildTopK = List.of();

        while (depth < maxDepth) {
            List<Task> nodes = depthBuckets.getOrDefault(depth, List.of());
            if (nodes.isEmpty()) break;

            // 1) P = 本層 top-k
            List<Task> P = retrieveTopK(nodes, qVec, k);
            lastParentTopK = P;

            // 2) ChildSet(P)
            List<Task> childSet = new ArrayList<>();
            for (Task p : P) {
                childSet.addAll(childrenMap.getOrDefault(p.getId(), List.of()));
            }

            // 3) U = P ∪ ChildSet(P)（用 id 去重）
            Map<String, Task> unionMap = new LinkedHashMap<>();
            for (Task t : P) unionMap.put(t.getId(), t);
            for (Task t : childSet) unionMap.put(t.getId(), t);
            List<Task> union = new ArrayList<>(unionMap.values());

            // 如果 union 只有 P（沒有孩子），那 ratio = 1，通常就可以選 P 裡最像的直接回
            if (union.size() == P.size()) {
                Task chosen = chooseParent(P, qVec);
                return Suggestion.placeUnder(chosen.getId(), depth, 1.0, P, List.of());
            }

            // 4) P' = top-k'(U)
            int kk = Math.min(kPrime, union.size());
            List<Task> Pprime = retrieveTopK(union, qVec, kk);
            lastChildTopK = Pprime; // 你要當 evidence 回傳也行（這其實是 top-k'）

            // 5) ratio = |P ∩ P'| / |P|
            Set<String> pIds = P.stream().map(Task::getId).collect(Collectors.toSet());
            long inter = Pprime.stream().filter(t -> pIds.contains(t.getId())).count();
            double ratio = (double) inter / Math.max(1, P.size());
            lastRatio = ratio;

            if (ratio >= M) {
                depth++;           // 交集大：孩子沒有搶走排名 → 往更深找
            } else {
                Task chosen = chooseParent(P, qVec); // 交集小：孩子很多更像 → 停在這層，parent 從 P 選
                return Suggestion.placeUnder(chosen.getId(), depth, ratio, P, Pprime);
            }
        }


        return Suggestion.placeUnder(rootId, depth, lastRatio, lastParentTopK, lastChildTopK);
    }

    /* ================= helpers ================= */

    private Map<Integer, List<Task>> buildDepthBuckets(Map<String, List<Task>> childrenMap, String rootId) {
        Map<Integer, List<Task>> buckets = new HashMap<>();
        List<Task> cur = childrenMap.getOrDefault(rootId, List.of());
        int d = 0;
        while (!cur.isEmpty()) {
            buckets.put(d, cur);
            List<Task> next = new ArrayList<>();
            for (Task t : cur) {
                next.addAll(childrenMap.getOrDefault(t.getId(), List.of()));
            }
            cur = next;
            d++;
        }
        return buckets;
    }

    private List<Task> retrieveTopK(List<Task> nodes, double[] qVec, int k) {
        return nodes.stream()
                .map(t -> new ScoredTask(t, cosine(getEmbedding(t), qVec)))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(k)
                .map(st -> st.task)
                .collect(Collectors.toList());
    }

    private Task chooseParent(List<Task> candidates, double[] qVec) {
        return candidates.stream()
                .max(Comparator.comparingDouble(t -> cosine(getEmbedding(t), qVec)))
                .orElseThrow();
    }

    private double[] getEmbedding(Task t) {
        return embeddingCache.computeIfAbsent(t.getId(),
                __ -> embedToDoubleArray(textOf(t)));
    }

    private double[] embedToDoubleArray(String text) {
        float[] vec = embeddingModel
                .embed(TextSegment.from(text))
                .content()
                .vector();

        double[] out = new double[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = vec[i];
        return out;
    }

    private String textOf(Task t) {
        String label = t.getData() != null ? Optional.ofNullable(t.getData().getLabel()).orElse("") : "";
        String desc  = t.getData() != null ? Optional.ofNullable(t.getData().getDescription()).orElse("") : "";
        return (label + " " + desc).trim();
    }

    private double cosine(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0 ? 0.0 : dot / denom;
    }

    private static class ScoredTask {
        final Task task;
        final double score;
        ScoredTask(Task task, double score) {
            this.task = task;
            this.score = score;
        }
    }

    public record Suggestion(
            String parentId,
            int depth,
            double ratio,
            List<Task> parentTopK,
            List<Task> childTopK
    ) {
        public static Suggestion placeUnder(String parentId, int depth, double ratio,
                                            List<Task> parentTopK, List<Task> childTopK) {
            return new Suggestion(parentId, depth, ratio, parentTopK, childTopK);
        }
    }
}
