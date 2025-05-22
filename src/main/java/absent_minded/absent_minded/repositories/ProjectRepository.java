package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {}
