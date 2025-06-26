package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {

    private final ProjectRepository repo;
    private final AuthService auth;

    public ProjectService(ProjectRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    public Project getProjectById(String header, String id) {
        String email = auth.emailFromAuthHeader(header);
        return repo.findByIdAndOwnerId(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}