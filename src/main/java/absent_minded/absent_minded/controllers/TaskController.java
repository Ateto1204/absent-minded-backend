package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.services.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> getAllTasks(@RequestHeader("Authorization") String header) {
        return service.getAllTasks(header);
    }

    @GetMapping("/project/{projectId}")
    public List<Task> getByProject(@PathVariable String projectId,
                                   @RequestHeader("Authorization") String header) {
        return service.getTasksByProject(header, projectId);
    }

    @PostMapping
    public List<Task> createTasks(@RequestHeader("Authorization") String header,
                             @RequestBody List<Task> tasks) {
        return service.createTasks(header, tasks);
    }

    @PutMapping
    public List<Task> updateTasks(@RequestHeader("Authorization") String header,
                             @RequestBody List<Task> tasks) {
        return service.updateTasks(header, tasks);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTasks(@RequestHeader("Authorization") String header,
                       @RequestBody List<String> ids) {
        service.deleteTasks(header, ids);
    }
}