package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.User;
import absent_minded.absent_minded.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public User getUserById(@RequestHeader("Authorization") String header) {
        return service.getUserById(header);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestHeader("Authorization") String header) {
        return service.createUser(header);
    }

    @PutMapping("/plan")
    public User updateUserPlan(@RequestHeader("Authorization") String header,
                               @RequestBody Map<String, String> body) {
        return service.updateUserPlan(header, body);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@RequestHeader("Authorization") String header) {
        service.deleteUser(header);
    }

    @PostMapping("/token-usage")
    public User addTokenUsage(@RequestHeader("Authorization") String header,
                              @RequestBody Map<String, Integer> body) {
        int tokens = body.getOrDefault("tokens", 0);
        return service.addTokenUsage(header, tokens);
    }
}