package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import absent_minded.absent_minded.services.AuthService;
import absent_minded.absent_minded.services.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repo;
    private final AuthService       auth;
    private final ProjectService service;

    public ProjectController(
            ProjectRepository repo,
            AuthService auth,
            ProjectService service) {
        this.repo = repo;
        this.auth = auth;
        this.service = service;
    }

    @GetMapping
    public List<Project> getAllProjects(@RequestHeader("Authorization") String authHeader) {
        String email = auth.emailFromAuthHeader(authHeader);
        Set<Project> allProjects = new HashSet<>();

        allProjects.addAll(repo.findAllByOwnerId(email));
        allProjects.addAll(repo.findAllByParticipantsContains(email));

        return new ArrayList<>(allProjects);
    }

    @GetMapping("/{id}")
    public Project getById(@RequestHeader("Authorization") String header,
                           @PathVariable String id) {
        return service.getProjectById(header, id);
    }

    @PostMapping
    public Project createProject(@RequestHeader("Authorization") String authHeader,
                          @RequestBody Project project) {
        String email = auth.emailFromAuthHeader(authHeader);

        if (project.getId() == null || project.getId().isBlank()) {
            project.setId(UUID.randomUUID().toString());
        }

        project.setOwnerId(email);
        return repo.save(project);
    }

    @PutMapping("/{id}")
    public Project updateProject(@RequestHeader("Authorization") String authHeader,
                          @PathVariable String id,
                          @RequestBody Project project) {
        String email = auth.emailFromAuthHeader(authHeader);

        Project original = repo.findByIdAndOwnerId(id, email)
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

        int deleted = repo.deleteByIdAndOwnerId(id, email);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    @PostMapping("/{id}/participants")
    public Project addParticipant(@RequestHeader("Authorization") String authHeader,
                                  @PathVariable String id,
                                  @RequestBody Map<String, String> body) {

        String requesterEmail = auth.emailFromAuthHeader(authHeader);
        Project project = repo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        /* 權限檢查：只有 owner 才能新增協作者 */
        if (!project.getOwnerId().equals(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can add participants");
        }

        String newParticipant = body.get("email");
        if (newParticipant == null || newParticipant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing participant email in request body");
        }

        /* 若 participants 為 null，先初始化 */
        if (project.getParticipants() == null) {
            project.setParticipants(new ArrayList<>());
        }

        /* 避免重複加入、避免把自己（owner）加入 */
        if (newParticipant.equals(project.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is already the project owner");
        }
        if (!project.getParticipants().contains(newParticipant)) {
            project.getParticipants().add(newParticipant);
        }

        return repo.save(project);
    }

    @DeleteMapping("/{id}/participants")
    public Project removeParticipant(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable String id,
                                     @RequestBody Map<String, String> body) {
        String requesterEmail = auth.emailFromAuthHeader(authHeader);
        Project project = repo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (!project.getOwnerId().equals(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can remove participants");
        }
        String participant = body.get("email");
        if (participant == null || participant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing participant email in request body");
        }
        if (project.getParticipants() != null && project.getParticipants().remove(participant)) {
            repo.save(project);
        }
        return project;
    }
}