package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskRepository repo;
    public TaskController(TaskRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Task> getAll() { return repo.findAll(); }

    @GetMapping("/{id}")
    public Task getById(@PathVariable String id) { return repo.findById(id).orElse(null); }

    @PostMapping
    public Task create(@RequestBody Task task) { return repo.save(task); }

    @PutMapping("/{id}")
    public Task update(@PathVariable String id, @RequestBody Task task) {
        task.setId(id);
        return repo.save(task);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { repo.deleteById(id); }
}
