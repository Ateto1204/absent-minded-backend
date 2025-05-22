package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository repo;
    public ProjectController(ProjectRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Project> getAll() { return repo.findAll(); }

    @GetMapping("/{id}")
    public Project getById(@PathVariable String id) { return repo.findById(id).orElse(null); }

    @PostMapping
    public Project create(@RequestBody Project project) { return repo.save(project); }

    @PutMapping("/{id}")
    public Project update(@PathVariable String id, @RequestBody Project project) {
        project.setId(id);
        return repo.save(project);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { repo.deleteById(id); }
}
