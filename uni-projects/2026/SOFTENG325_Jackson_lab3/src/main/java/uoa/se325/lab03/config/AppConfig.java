package uoa.se325.lab03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uoa.se325.lab03.service.LibraryDB;

@Configuration
@Profile("prod")
public class AppConfig {

    @Bean
    public LibraryDB libraryDB() {
        return new LibraryDB();
    }
}
