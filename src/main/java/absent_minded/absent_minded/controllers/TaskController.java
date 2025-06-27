package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import absent_minded.absent_minded.services.AuthService;
import absent_minded.absent_minded.services.TaskService;
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
    private final TaskService service;

    public TaskController(
            TaskRepository repo,
            AuthService auth,
            TaskService service) {
        this.repo = repo;
        this.auth = auth;
        this.service = service;
    }

    @GetMapping
    public List<Task> getAll(@RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        Set<Task> allTasks = new HashSet<>();

        allTasks.addAll(repo.findAllByOwnerId(email));
        allTasks.addAll(repo.findAllByParticipantsContains(email));

        return new ArrayList<>(allTasks);
    }

    @GetMapping("/project/{projectId}")
    public List<Task> getByProject(@PathVariable String projectId,
                                   @RequestHeader("Authorization") String header) {
        return service.getTasksByProject(header, projectId);
    }

    @PostMapping
    public List<Task> create(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> {
            List<String> participants = t.getParticipants();
            if (!participants.contains(email)) {
                t.setOwnerId(email);
            }
        });
        return repo.saveAll(tasks);
    }

    @PutMapping
    public List<Task> update(@RequestHeader("Authorization") String authHeader,
                             @RequestBody List<Task> tasks) {
        String email = auth.emailFromAuthHeader(authHeader);
        tasks.forEach(t -> {
            List<String> participants = t.getParticipants();
            if (!participants.contains(email)) {
                t.setOwnerId(email);
            }
        });
        return repo.saveAll(tasks);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authHeader,
                       @RequestBody List<String> ids) {
        String email = auth.emailFromAuthHeader(authHeader);
        List<Task> tasks = repo.findAllById(ids);
        List<String> allowedToDelete = tasks.stream()
                .filter(t -> email.equals(t.getOwnerId()) || t.getParticipants().contains(email))
                .map(Task::getId)
                .toList();
        repo.deleteAllById(allowedToDelete);
    }
}