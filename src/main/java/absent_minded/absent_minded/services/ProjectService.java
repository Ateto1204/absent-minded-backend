package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.repositories.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository repo;
    private final AuthService auth;

    public ProjectService(ProjectRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    public Project getProjectById(String header, String projectId) {
        String visitor = auth.emailFromAuthHeader(header);
        Optional<Project> optionalProject = repo.findById(projectId);
        if (optionalProject.isEmpty()) {
            return null;
        }
        Project project = optionalProject.get();

        if (!project.getOwnerId().equals(visitor) &&
                (project.getParticipants() == null || !project.getParticipants().contains(visitor))) {
            return null;
        }
        return project;
    }
}