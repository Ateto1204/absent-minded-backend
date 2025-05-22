package absent_minded.absent_minded.models;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    private String id;
    private String name;
    private String userId;
    private String rootTask;
    // getter/setter
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String user) {
        this.userId = user;
    }

    public String getRootTask() {
        return rootTask;
    }

    public void setRootTask(String rootTask) {
        this.rootTask = rootTask;
    }
}
