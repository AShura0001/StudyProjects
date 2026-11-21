package uoa.se325.lab04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uoa.se325.lab04.model.pokemon.Trainer;

public interface TrainerRepository extends JpaRepository<Trainer, Integer> {
}
