package absent_minded.absent_minded.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String plan;

    @Column(name = "token_used", nullable = false)
    private int tokenUsed;

    @ElementCollection
    @CollectionTable(
            name = "user_events",
            joinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEvent> events = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime created;

    public User() {
        this.created = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public int getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(int tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    public List<UserEvent> getEvents() {
        return events;
    }

    public void setEvents(List<UserEvent> events) {
        this.events = events;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}