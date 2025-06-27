package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskService {

    private final TaskRepository repo;
    private final AuthService auth;
    private final ProjectService projectService;

    public TaskService(
            TaskRepository repo,
            AuthService auth,
            ProjectService projectService) {
        this.repo = repo;
        this.auth = auth;
        this.projectService = projectService;
    }

    public List<Task> getTasksByProject(String header, String projectId) {
        Project project = projectService.getProjectById(header, projectId);
        if (project == null) {
            return Collections.emptyList();
        }
        return repo.findAllByProject(projectId);
    }

    public List<Task> getAllTasks(String header) {
        String email = auth.emailFromAuthHeader(header);
        Set<Task> allTasks = new HashSet<>();

        allTasks.addAll(repo.findAllByOwnerId(email));
        allTasks.addAll(repo.findAllByParticipantsContains(email));

        return new ArrayList<>(allTasks);
    }
}