package absent_minded.absent_minded.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class UserEvent {

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public UserEvent() {

    }

    public UserEvent(String event) {
        this.event = event;
        this.timestamp = LocalDateTime.now();
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}