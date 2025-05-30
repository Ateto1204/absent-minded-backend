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

    // 1. 根據 userId 查詢 tasks
    @GetMapping("/user/{userId}")
    public List<Task> getTasksByUserId(@PathVariable String userId) {
        return repo.findByUserId(userId);
    }

    // 2. 根據 projectId 查詢 tasks
    @GetMapping("/project/{projectId}")
    public List<Task> getTasksByProjectId(@PathVariable String projectId) {
        return repo.findByProjectId(projectId);
    }

    // 3. 批次新增 tasks
    @PostMapping("/batch")
    public List<Task> createTasks(@RequestBody List<Task> tasks) {
        return repo.saveAll(tasks);
    }

    // 4. 批次刪除 tasks
    @DeleteMapping("/batch")
    public void deleteTasks(@RequestBody List<String> ids) {
        ids.forEach(repo::deleteById);
    }

    // 5. 批次更新 tasks
    @PutMapping("/batch")
    public List<Task> updateTasks(@RequestBody List<Task> tasks) {
        return repo.saveAll(tasks);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable String id, @RequestBody Task task) {
        task.setId(id);
        return repo.save(task);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { repo.deleteById(id); }
}
