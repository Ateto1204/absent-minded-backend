package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByUserId(String userId);

    Optional<Project> findByIdAndUserId(String id, String userId);

    int deleteByIdAndUserId(String id, String userId);
}
