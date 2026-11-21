package uoa.se325.lab03.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uoa.se325.lab03.model.library.*;
import uoa.se325.lab03.service.LibraryDB;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;

@TestConfiguration
@Profile("test")
public class TestConfig {

    private final BufferedImage blackWidowImage;
    private final BufferedImage twentyThousandLeaguesImage;

    public TestConfig() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (var in = classLoader.getResourceAsStream("black-widow.png")) {
            blackWidowImage = ImageIO.read(in);
        }
        try (var in = classLoader.getResourceAsStream("twenty-thousand-leagues.png")) {
            twentyThousandLeaguesImage = ImageIO.read(in);
        }
    }

    @Bean
    public LibraryDB libraryDB() {
        var db = new LibraryDB();

        var joeBloggs = new User("12345", LocalDate.of(1986, 3, 17),
                LocalDate.of(2015, 1, 1), "Joe Bloggs");

        var twentyThousandLeagues = new Book("000000001", "Twenty Thousand Leagues Under the Sea", 364, "Jules Verne", "Pierre-Jules Hetzel", Genre.Adventure, Genre.SciFi);
        twentyThousandLeagues.setCoverImage(twentyThousandLeaguesImage);

        var blackWidow = new Film("000000002", "Black Widow", Rating.M, 134, "Cate Shortland", Genre.Action, Genre.Adventure, Genre.SciFi);
        blackWidow.setCoverImage(blackWidowImage);

        var library = db.getLibrary();
        library.addUser(joeBloggs);
        library.addToInventory(twentyThousandLeagues, 3);
        library.addToInventory(blackWidow, 1);

        return db;
    }
}
