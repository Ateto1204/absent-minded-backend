package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.User;
import absent_minded.absent_minded.models.UserEvent;
import absent_minded.absent_minded.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository repo;
    private final AuthService auth;

    public UserService(UserRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    public User getUserById(String header) {
        String requester = auth.emailFromAuthHeader(header);
        return repo.findById(requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUser(String header, User user) {
        user.setCreated(LocalDateTime.now());
        if (user.getPlan() == null) {
            user.setPlan("free");
        }
        if (user.getEvents() == null) {
            user.setEvents(List.of());
        }
        user.setTokenUsed(0);
        return repo.save(user);
    }

    public User updateUserPlan(String header, Map<String, String> body) {
        String requester = auth.emailFromAuthHeader(header);
        User original = repo.findById(requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String plan = body.get("plan");
        original.setPlan(plan);
        return repo.save(original);
    }

    public void deleteUser(String header) {
        String requester = auth.emailFromAuthHeader(header);
        if (!repo.existsById(requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        repo.deleteById(requester);
    }

    public User addTokenUsage(String header, int tokens) {
        User user = getUserById(header);
        user.setTokenUsed(user.getTokenUsed() + tokens);
        return repo.save(user);
    }

    public User addEvent(String header, Map<String, String> body) {
        User user = getUserById(header);
        String event = body.get("event");
        if (event == null || event.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing history entry");
        }
        if (user.getEvents() == null) {
            user.setEvents(new java.util.ArrayList<>());
        }
        UserEvent userEvent = new UserEvent(event);
        user.getEvents().add(userEvent);
        return repo.save(user);
    }
}
