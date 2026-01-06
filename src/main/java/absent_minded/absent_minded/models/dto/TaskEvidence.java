package absent_minded.absent_minded.models.dto;

public record TaskEvidence(
        String id,
        String label,
        String description,
        String parentId,
        int depth
) {}

