package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import absent_minded.absent_minded.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repo;
    private final AuthService       auth;

    public ProjectController(ProjectRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    @GetMapping
    public List<Project> getAll(@RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        return repo.findAllByUserId(email);
    }

    @GetMapping("/{id}")
    public Project getById(@RequestHeader("Authorization") String authHeader,
                           @PathVariable String id) {

        String email = auth.emailFromAuthHeader(authHeader);
        return repo.findByIdAndUserId(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @PostMapping
    public Project create(@RequestHeader("Authorization") String authHeader,
                          @RequestBody Project project) {

        String email = auth.emailFromAuthHeader(authHeader);

        if (project.getId() == null || project.getId().isBlank()) {
            project.setId(UUID.randomUUID().toString());
        }
        project.setUserId(email);
        return repo.save(project);
    }

    @PutMapping("/{id}")
    public Project update(@RequestHeader("Authorization") String authHeader,
                          @PathVariable String id,
                          @RequestBody Project project) {

        String email = auth.emailFromAuthHeader(authHeader);

        Project original = repo.findByIdAndUserId(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        original.setName(project.getName());
        original.setRootTask(project.getRootTask());

        return repo.save(original);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authHeader,
                       @PathVariable String id) {

        String email = auth.emailFromAuthHeader(authHeader);

        int deleted = repo.deleteByIdAndUserId(id, email);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }
}