// TaskRepository.java
package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findAllByUserId(String userId);

    List<Task> findAllByUserIdAndProject(String userId, String project);

    @Transactional
    @Modifying
    void deleteAllByIdInAndUserId(Iterable<String> ids, String userId);
}