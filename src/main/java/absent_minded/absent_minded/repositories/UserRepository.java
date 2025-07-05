package absent_minded.absent_minded.repositories;

import absent_minded.absent_minded.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
