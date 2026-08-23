package uoa.se325.lab04.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uoa.se325.lab04.service.TrainerDB;

@Configuration
@Profile("prod")
public class AppConfig {

    @Bean
    public TrainerDB trainerDB() {
        return new TrainerDB();
    }
}
