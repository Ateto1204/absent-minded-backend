package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {}
