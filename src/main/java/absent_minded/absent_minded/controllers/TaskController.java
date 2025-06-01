package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import absent_minded.absent_minded.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<Task> getTasksByOwnerId(@RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        Set<Task> allTasks = new HashSet<>();

        allTasks.addAll(repo.findAllByOwnerId(email));
        allTasks.addAll(repo.findAllByParticipantsContains(email));

        return new ArrayList<>(allTasks);
    }

    @GetMapping("/project/{projectId}")
    public List<Task> getByProject(@PathVariable String projectId,
                                   @RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        return repo.findAllByOwnerIdAndProject(email, projectId);
    }

    @PostMapping
    public List<Task> create(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> t.setOwnerId(email));
        return repo.saveAll(tasks);
    }

    @PutMapping
    public List<Task> update(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> t.setOwnerId(email));
        return repo.saveAll(tasks);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authHeader,
                       @RequestBody List<String> ids) {
        String email = auth.emailFromAuthHeader(authHeader);
        repo.deleteAllByIdInAndOwnerId(ids, email);
    }
}