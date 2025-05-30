package absent_minded.absent_minded.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    private String id;
    private String project;
    private String parent;
    private String userId;

    @Embedded
    private TaskData data;

    private String status;

    // getter/setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public TaskData getData() {
        return data;
    }

    public void setData(TaskData data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}