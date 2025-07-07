package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class ProjectService {

    private final ProjectRepository repo;
    private final AuthService auth;

    public ProjectService(ProjectRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    public List<Project> getAllProjects(String header) {
        String email = auth.emailFromAuthHeader(header);
        Set<Project> allProjects = new HashSet<>();

        allProjects.addAll(repo.findAllByOwnerId(email));
        allProjects.addAll(repo.findAllByParticipantsContains(email));

        return new ArrayList<>(allProjects);
    }

    public Project getProjectById(String header, String projectId) {
        String visitor = auth.emailFromAuthHeader(header);
        Optional<Project> optionalProject = repo.findById(projectId);
        if (optionalProject.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "project does not exist");
        }
        Project project = optionalProject.get();

        if (!project.getOwnerId().equals(visitor) &&
                (project.getParticipants() == null || !project.getParticipants().contains(visitor))) {
            return null;
        }
        return project;
    }

    public Project createProject(String header, Project project) {
        String email = auth.emailFromAuthHeader(header);

        if (project.getId() == null || project.getId().isBlank()) {
            project.setId(UUID.randomUUID().toString());
        }

        project.setOwnerId(email);
        return repo.save(project);
    }

    public Project updateProject(String header, String id, Project project) {
        String email = auth.emailFromAuthHeader(header);

        Project original = repo.findByIdAndOwnerId(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        original.setName(project.getName());
        original.setRootTask(project.getRootTask());

        return repo.save(original);
    }

    public void deleteProject(String header, String id) {
        String email = auth.emailFromAuthHeader(header);

        int deleted = repo.deleteByIdAndOwnerId(id, email);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    public Project addParticipant(String header, String id, Map<String, String> body) {
        String visitor = auth.emailFromAuthHeader(header);
        Project project = getProjectById(header, id);

        /* auth check：only owner can add member */
        String projectOwner = project.getOwnerId();
        if (!projectOwner.equals(visitor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can add participants");
        }

        String newParticipant = body.get("email");
        if (newParticipant == null || newParticipant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing participant email in request body");
        }

        /* if participants were null, it should initialize */
        if (project.getParticipants() == null) {
            project.setParticipants(new ArrayList<>());
        }

        /* avoid to add repeatedly or add self accidentally */
        if (newParticipant.equals(project.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is already the project owner");
        }
        if (!project.getParticipants().contains(newParticipant)) {
            project.getParticipants().add(newParticipant);
        }

        return repo.save(project);
    }

    public Project removeParticipant(String header, String id, Map<String, String> body) {
        String requesterEmail = auth.emailFromAuthHeader(header);
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