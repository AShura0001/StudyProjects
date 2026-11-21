package uoa.se325.lab04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uoa.se325.lab04.model.concert.Performer;

public interface PerformerRepository extends JpaRepository<Performer, Long> {
}
