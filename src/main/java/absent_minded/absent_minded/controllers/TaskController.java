package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import absent_minded.absent_minded.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repo;
    private final AuthService     auth;

    public TaskController(TaskRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    @GetMapping
    public List<Task> getTasksByUserId(@RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        List<Task> tasks = repo.findAllByUserId(email);
        if (tasks == null) return java.util.Collections.emptyList();
        return tasks;
    }

    @GetMapping("/project/{projectId}")
    public List<Task> getByProject(@PathVariable String projectId,
                                   @RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        return repo.findAllByUserIdAndProject(email, projectId);
    }

    @PostMapping
    public List<Task> create(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> t.setUserId(email));
        return repo.saveAll(tasks);
    }

    @PutMapping
    public List<Task> update(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> t.setUserId(email));
        return repo.saveAll(tasks);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authHeader,
                       @RequestBody List<String> ids) {
        String email = auth.emailFromAuthHeader(authHeader);
        repo.deleteAllByIdInAndUserId(ids, email);
    }
}