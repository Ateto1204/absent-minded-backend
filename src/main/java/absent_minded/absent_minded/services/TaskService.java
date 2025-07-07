package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        List<Project> projects = projectService.getAllProjects(header);
        List<String> projectIds = projects.stream()
                .map(Project::getId)
                .toList();
        return repo.findAllByProjectIn(projectIds);
    }

    public List<Task> createTasks(String header, List<Task> tasks) {
        verifyTasksVisitor(header, tasks);
        return repo.saveAll(tasks);
    }

    public List<Task> updateTasks(String header, List<Task> tasks) {
        verifyTasksVisitor(header, tasks);
        return repo.saveAll(tasks);
    }

    public void deleteTasks(String header, List<String> ids) {
        verifyIdsVisitor(header, ids);
        repo.deleteAllById(ids);
    }

    private void verifyTasksVisitor(String header, List<Task> tasks) {
        String email = auth.emailFromAuthHeader(header);
        tasks.forEach(t -> {
            Project project = projectService.getProjectById(header, t.getProject());
            if (!project.getOwnerId().equals(email) && !project.getParticipants().contains(email)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO access");
            }
        });
    }

    private void verifyIdsVisitor(String header, List<String> ids) {
        String email = auth.emailFromAuthHeader(header);
        ids.forEach(id -> {
            Project project = projectService.getProjectById(header, id);
            if (!project.getOwnerId().equals(email) && !project.getParticipants().contains(email)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO access");
            }
        });
    }
}