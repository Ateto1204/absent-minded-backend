package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findAllByOwnerId(String ownerId);
    List<Task> findAllByProject(String project);
    List<Task> findAllByProjectIn(List<String> projectIds);
}