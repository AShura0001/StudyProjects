package uoa.se325.assignment01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uoa.se325.assignment01.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}
