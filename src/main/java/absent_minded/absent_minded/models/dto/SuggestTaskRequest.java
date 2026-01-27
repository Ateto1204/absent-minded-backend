package absent_minded.absent_minded.models.dto;

public record SuggestTaskRequest(
        String projectId,
        TaskInput task
) {
    public record TaskInput(
            String label,
            String description
    ) {}
}
