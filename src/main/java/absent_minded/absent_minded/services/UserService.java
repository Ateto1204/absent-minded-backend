package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.User;
import absent_minded.absent_minded.models.UserEvent;
import absent_minded.absent_minded.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public User createUser(String header) {
        String requester = auth.emailFromAuthHeader(header);
        User user = new User(requester);
        return repo.save(user);
    }

    public User updateUserPlan(String header, Map<String, String> body) {
        String requester = auth.emailFromAuthHeader(header);
        User user = repo.findById(requester)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String plan = body.get("plan");

        if (!user.getPlan().equals(plan)) {
            user.setPlan(plan);
            String event = "No mention";
            switch (plan) {
                case "pro" -> event = "Upgrade to pro plan";
                case "free" -> event = "Downgrade to free plan";
            }
            addEvent(user, event);
            return repo.save(user);
        }

        return user;
    }

    public void deleteUser(String header) {
        String requester = auth.emailFromAuthHeader(header);
        if (!repo.existsById(requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        repo.deleteById(requester);
    }

    public void addTokenUsage(String header, String prompt) {
        User user = getUserById(header);
        user.setTokenUsed(user.getTokenUsed() + prompt.length());
        repo.save(user);
    }

    private void addEvent(User user, String event) {
        if (user.getEvents() == null) {
            user.setEvents(new java.util.ArrayList<>());
        }
        UserEvent userEvent = new UserEvent(event);
        user.getEvents().add(userEvent);
    }
}
