package uoa.se325.lab04.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uoa.se325.lab04.model.pokemon.Pokemon;
import uoa.se325.lab04.model.pokemon.Trainer;
import uoa.se325.lab04.service.TrainerDB;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public TrainerDB trainerDB() {
        TrainerDB db = new TrainerDB();


        // Add Trainer Ash Ketchum with Pokémon
        db.addTrainer(new Trainer(
                "Ash Ketchum",
                1,
                new Pokemon("Pikachu", "Sparky", 100),
                new Pokemon("Charizard", "Fireball", 76)
        ));

        // Add Trainer Andrew with Pokémon
        db.addTrainer(new Trainer(
                "Andrew",
                2,
                new Pokemon("Dragonite", "Yoshi", 255)
        ));

        return db;
        
    }
}
