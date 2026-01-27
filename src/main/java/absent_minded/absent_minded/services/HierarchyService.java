package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;


@Service
public class HierarchyService {

    private static final Logger log = LoggerFactory.getLogger(HierarchyService.class);

    private final TaskRepository taskRepo;
    private final ProjectService projectService;

    // ✅ 固定用本地 BGE（不走 OpenAI）
    private final EmbeddingModel embeddingModel;

    // ✅ embedding cache
    private final Map<String, double[]> embeddingCache = new ConcurrentHashMap<>();

    private static final int TRACE_CANDIDATE_LIMIT = 20;

    public HierarchyService(TaskRepository taskRepo,
                            ProjectService projectService) {
        this.taskRepo = taskRepo;
        this.projectService = projectService;
        this.embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();

        log.warn("[HIERARCHY] EmbeddingModel impl = {}", this.embeddingModel.getClass().getName());
    }

    public Suggestion suggestParent(String header,
                                    String projectId,
                                    String newTaskText,
                                    int k,
                                    int kPrime,
                                    double M,
                                    int maxDepth) {

        Project project = projectService.getProjectById(header, projectId);
        String rootId = (project == null) ? null : project.getRootTask();

        List<Task> all = taskRepo.findAllByProject(projectId);
        String queryPreview = Optional.ofNullable(newTaskText).orElse("");
        if (queryPreview.length() > 80) {
            queryPreview = queryPreview.substring(0, 80) + "...";
        }
        log.info("[HIERARCHY] suggestParent projectId={} tasks={} depthLimit={} k={} kPrime={} M={} queryPreview='{}'",
                projectId, all.size(), maxDepth, k, kPrime, M, queryPreview);

        if (rootId == null || rootId.isBlank()) {
            log.warn("[HIERARCHY] rootId is missing/blank | projectId={} | rootId='{}' | allTasks={}",
                    projectId, rootId, all.size());

            // ✅ fallback：即便沒有 root，也要給 evidence，避免 LLM 一直說沒有檢索到
            double[] qVec = embedToDoubleArray(newTaskText);
            int kk = Math.min(k, all.size());
            List<Task> fallbackTopK = (kk > 0) ? retrieveTopK(all, qVec, kk) : List.of();

            Suggestion result = Suggestion.placeUnder(null, 0, 0.0, fallbackTopK, List.of(), List.of());
            logSuggestionResult(projectId, result, "root-missing");
            return result;
        }


        Map<String, List<Task>> childrenMap = new HashMap<>();
        Map<String, Task> byId = new HashMap<>();
        for (Task t : all) {
            byId.put(t.getId(), t);
            if (t.getParent() != null) {
                childrenMap.computeIfAbsent(t.getParent(), __ -> new ArrayList<>()).add(t);
            }
        }

        if (!byId.containsKey(rootId)) {

            Suggestion result = Suggestion.placeUnder(null, 0, 0.0, List.of(), List.of(), List.of());
            logSuggestionResult(projectId, result, "root-not-found");
            return result;
        }

        Map<Integer, List<Task>> depthBuckets = buildDepthBuckets(childrenMap, rootId);

        // ✅ query embedding
        double[] qVec = embedToDoubleArray(newTaskText);

        int depth = 0;
        double lastRatio = 0.0;
        List<Task> lastParentTopK = List.of();
        List<Task> lastChildTopK = List.of();

        // ✅ trace 收集器
        List<StepTrace> traces = new ArrayList<>();

        while (depth < maxDepth) {
            List<Task> nodes = depthBuckets.getOrDefault(depth, List.of());
            if (nodes.isEmpty()) {
                // ✅ fallback：如果樹斷/這層沒有節點，就改用整個 project 的 tasks 當候選
                nodes = all.stream()
                        .filter(t -> t.getId() != null && !t.getId().equals(rootId))
                        .toList();
                if (nodes.isEmpty()) break;
                log.debug("[HIERARCHY] depth {} fallback to {} project-wide nodes", depth, nodes.size());
            }


            // 1) P = 本層 top-k
            List<Task> P = retrieveTopK(nodes, qVec, k);
            lastParentTopK = P;

            // 2) ChildSet(P)
            List<Task> childSet = new ArrayList<>();
            for (Task p : P) {
                log.info(p.getId());
                childSet.addAll(childrenMap.getOrDefault(p.getId(), List.of()));
            }
            log.info("====================");

            // 3) U = P ∪ ChildSet(P)（用 id 去重）
            Map<String, Task> unionMap = new LinkedHashMap<>();
            for (Task t : P) unionMap.put(t.getId(), t);
            for (Task t : childSet) unionMap.put(t.getId(), t);
            List<Task> union = new ArrayList<>(unionMap.values());

            // 如果 union 只有 P（沒有孩子），那 ratio = 1，通常就可以選 P 裡最像的直接回
            if (union.size() == P.size()) {
                log.info("[HIERARCHY] depth {} union size == P size (no children) → choose from P directly", depth);
                // trace：最後一輪也記下
                StepTrace trace = StepTrace.from(
                        depth,
                        topHits(nodes, qVec, TRACE_CANDIDATE_LIMIT),
                        hitsOf(P, qVec),
                        topHits(union, qVec, TRACE_CANDIDATE_LIMIT),
                        List.of(), // 沒有 P'
                        1.0
                );
                traces.add(trace);
                logTraceStep(trace);

                Task chosen = chooseParent(P, qVec);
                Suggestion result = Suggestion.placeUnder(chosen.getId(), depth, 1.0, P, List.of(), traces);
                logSuggestionResult(projectId, result, "no-children-at-depth");
                return result;
            }

            // 4) P' = top-k'(U)
            int kk = Math.min(kPrime, union.size());
            List<Task> Pprime = retrieveTopK(union, qVec, kk);
            lastChildTopK = Pprime;

            // 5) ratio = |P ∩ P'| / |P|
            Set<String> pIds = P.stream().map(Task::getId).collect(Collectors.toSet());
            long inter = Pprime.stream().filter(t -> pIds.contains(t.getId())).count();
            double ratio = (double) inter / Math.max(1, P.size());
            lastRatio = ratio;

            // ✅ 每輪記錄 trace（nodes/union 用 topHits 限制數量；P/P' 都是小集合可全存）
            StepTrace trace = StepTrace.from(
                    depth,
                    topHits(nodes, qVec, TRACE_CANDIDATE_LIMIT),
                    hitsOf(P, qVec),
                    topHits(union, qVec, TRACE_CANDIDATE_LIMIT),
                    hitsOf(Pprime, qVec),
                    ratio
            );
            traces.add(trace);
            logTraceStep(trace);

            if (ratio <= M) {
                log.debug("[HIERARCHY] depth {} ratio {} >= M {} → descend", depth,
                        String.format(Locale.US, "%.3f", ratio), String.format(Locale.US, "%.3f", M));
                depth++;           // 交集大：孩子沒有搶走排名 → 往更深找
            } else {
                Task chosen = chooseParent(P, qVec); // 交集小：孩子很多更像 → 停在這層，parent 從 P 選
                Suggestion result = Suggestion.placeUnder(chosen.getId(), depth, ratio, P, Pprime, traces);
                logSuggestionResult(projectId, result, "ratio-below-threshold");
                return result;
            }
        }

        // 走到底：回 rootId + traces
        Suggestion result = Suggestion.placeUnder(rootId, depth, lastRatio, lastParentTopK, lastChildTopK, traces);
        logSuggestionResult(projectId, result, "max-depth");
        return result;
    }

    /* ================= trace records ================= */

    /** 一筆 task 比對結果：你就能看到到底比對到誰 + 分數 */
    public record TaskHit(
            String id,
            String parentId,
            String label,
            double score
    ) {}

    /** 每一輪 depth 的 trace */
    public record StepTrace(
            int depth,
            List<TaskHit> nodesTop,   // 本層候選(只保留前 N 個最像的)
            List<TaskHit> pTopK,      // P(top-k)
            List<TaskHit> unionTop,   // union(只保留前 N 個最像的)
            List<TaskHit> pPrimeTopK, // P'(top-k')
            double ratio
    ) {
        public static StepTrace from(int depth,
                                     List<TaskHit> nodesTop,
                                     List<TaskHit> pTopK,
                                     List<TaskHit> unionTop,
                                     List<TaskHit> pPrimeTopK,
                                     double ratio) {
            return new StepTrace(depth, nodesTop, pTopK, unionTop, pPrimeTopK, ratio);
        }
    }

    public record Suggestion(
            String parentId,
            int depth,
            double ratio,
            List<Task> parentTopK,
            List<Task> childTopK,
            List<StepTrace> traces
    ) {
        public static Suggestion placeUnder(String parentId, int depth, double ratio,
                                            List<Task> parentTopK, List<Task> childTopK,
                                            List<StepTrace> traces) {
            return new Suggestion(parentId, depth, ratio, parentTopK, childTopK, traces);
        }
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

    /** 把一組 tasks 轉成 hits（不限制數量） */
    private List<TaskHit> hitsOf(List<Task> tasks, double[] qVec) {
        return tasks.stream()
                .map(t -> new TaskHit(
                        t.getId(),
                        t.getParent(),
                        safeLabel(t),
                        cosine(getEmbedding(t), qVec)
                ))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    /** 對大量 tasks 只保留前 limit 個最像的（避免 trace 太大） */
    private List<TaskHit> topHits(List<Task> tasks, double[] qVec, int limit) {
        return tasks.stream()
                .map(t -> new TaskHit(
                        t.getId(),
                        t.getParent(),
                        safeLabel(t),
                        cosine(getEmbedding(t), qVec)
                ))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .toList();
    }

    private String safeLabel(Task t) {
        String label = (t.getData() != null) ? Optional.ofNullable(t.getData().getLabel()).orElse("") : "";
        return label.replace("\n", " ").trim();
    }

    private void logTraceStep(StepTrace trace) {
        if (!log.isDebugEnabled() || trace == null) return;
        log.debug("[HIERARCHY][trace] depth={} ratio={} nodes={} P={} union={} P'={}",
                trace.depth(),
                String.format(Locale.US, "%.3f", trace.ratio()),
                formatHits(trace.nodesTop()),
                formatHits(trace.pTopK()),
                formatHits(trace.unionTop()),
                formatHits(trace.pPrimeTopK()));
    }

    private void logSuggestionResult(String projectId, Suggestion suggestion, String reason) {
        if (!log.isInfoEnabled() || suggestion == null) return;
        log.info("[HIERARCHY][result] projectId={} reason={} parentId={} depth={} ratio={} parentTopK={} childTopK={} traceCount={}",
                projectId,
                reason,
                suggestion.parentId(),
                suggestion.depth(),
                String.format(Locale.US, "%.3f", suggestion.ratio()),
                formatTasks(suggestion.parentTopK()),
                formatTasks(suggestion.childTopK()),
                suggestion.traces() == null ? 0 : suggestion.traces().size());
    }

    private String formatHits(List<TaskHit> hits) {
        if (hits == null || hits.isEmpty()) return "[]";
        return hits.stream()
                .limit(3)
                .map(hit -> {
                    String label = (hit.label() == null || hit.label().isBlank()) ? hit.id() : hit.label();
                    return label + "/" + hit.id() + String.format(Locale.US, "(%.2f)", hit.score());
                })
                .collect(Collectors.joining(", ", "[", hits.size() > 3 ? ", ...]" : "]"));
    }

    private String formatTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return "[]";
        return tasks.stream()
                .limit(3)
                .map(t -> safeLabel(t) + "/" + t.getId())
                .collect(Collectors.joining(", ", "[", tasks.size() > 3 ? ", ...]" : "]"));
    }
}
