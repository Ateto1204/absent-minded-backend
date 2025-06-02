package absent_minded.absent_minded.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    private String id;
    private String name;

    private String ownerId;

    private String rootTask;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_participants",
            joinColumns = @JoinColumn(name = "project_id")
    )
    private List<String> participants = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getRootTask() {
        return rootTask;
    }

    public void setRootTask(String rootTask) {
        this.rootTask = rootTask;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}