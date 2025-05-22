package absent_minded.absent_minded.models;

import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class TaskData {
    private String label;
    private String description;
    private LocalDateTime start;
    private LocalDateTime deadline;
    // getter/setter
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
}
