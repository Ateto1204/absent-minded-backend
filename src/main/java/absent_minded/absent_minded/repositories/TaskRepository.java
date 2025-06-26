package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findAllByOwnerId(String ownerId);
    List<Task> findAllByOwnerIdAndProject(String ownerId, String project);
    List<Task> findAllByProject(String project);

    @Transactional
    @Modifying
    void deleteAllByIdInAndOwnerId(Iterable<String> ids, String ownerId);

    List<Task> findAllByParticipantsContains(String email);
    List<Task> findAllByParticipantsContainsAndProject(String email, String project);
}