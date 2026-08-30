package uoa.se325.lab05.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uoa.se325.lab05.model.Concert;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
}
