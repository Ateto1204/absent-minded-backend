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

    public List<Task> createTasks(String header, List<Task> tasks) {
        String email = auth.emailFromAuthHeader(header);
        tasks.forEach(t -> {
            List<String> participants = t.getParticipants();
            if (!participants.contains(email)) {
                t.setOwnerId(email);
            }
        });
        return repo.saveAll(tasks);
    }

    public List<Task> updateTasks(String header, List<Task> tasks) {
        String email = auth.emailFromAuthHeader(header);
        tasks.forEach(t -> {
            List<String> participants = t.getParticipants();
            if (!participants.contains(email)) {
                t.setOwnerId(email);
            }
        });
        return repo.saveAll(tasks);
    }

    public void deleteTasks(String header, List<String> ids) {
        String email = auth.emailFromAuthHeader(header);
        List<Task> tasks = repo.findAllById(ids);
        List<String> allowedToDelete = tasks.stream()
                .filter(t -> email.equals(t.getOwnerId()) || t.getParticipants().contains(email))
                .map(Task::getId)
                .toList();
        repo.deleteAllById(allowedToDelete);
    }
}