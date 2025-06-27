package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.services.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<Project> getAllProjects(@RequestHeader("Authorization") String header) {
        return service.getAllProjects(header);
    }

    @GetMapping("/{id}")
    public Project getProjectById(@RequestHeader("Authorization") String header,
                           @PathVariable String id) {
        return service.getProjectById(header, id);
    }

    @PostMapping
    public Project createProject(@RequestHeader("Authorization") String header,
                          @RequestBody Project project) {
        return service.createProject(header, project);
    }

    @PutMapping("/{id}")
    public Project updateProject(@RequestHeader("Authorization") String header,
                          @PathVariable String id,
                          @RequestBody Project project) {
        return service.updateProject(header, id, project);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@RequestHeader("Authorization") String header,
                       @PathVariable String id) {
        service.deleteProject(header, id);
    }

    @PostMapping("/{id}/participants")
    public Project addParticipant(@RequestHeader("Authorization") String header,
                                  @PathVariable String id,
                                  @RequestBody Map<String, String> body) {

        return service.addParticipant(header, id, body);
    }

    @DeleteMapping("/{id}/participants")
    public Project removeParticipant(@RequestHeader("Authorization") String header,
                                     @PathVariable String id,
                                     @RequestBody Map<String, String> body) {
        return service.removeParticipant(header, id, body);
    }
}