package uoa.se325.lab03.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uoa.se325.lab03.config.TestConfig;
import uoa.se325.lab03.model.library.Film;
import uoa.se325.lab03.model.library.Genre;
import uoa.se325.lab03.model.library.Rating;
import uoa.se325.lab03.service.LibraryDB;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InventoryControllerPaginationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryDB libraryDB;

    @Test
    public void testGetInventoryPaginationFirstPage() throws Exception {

        // Create 20 extra media in the DB first
        var library = libraryDB.getLibrary();
        for (int i = 1; i <= 20; i++) {
            library.addToInventory(
                    new Film("" + i, "Film#" + i, Rating.G, i, "SE325", Genre.Action),
                    1);
        }

        mockMvc.perform(get("/inventory?page=1&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Prev-Page"))
                .andExpect(header().exists("Next-Page"))
                .andExpect(header().longValue("Next-Page", 2))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$[1].media.title").value("Black Widow"))
                .andExpect(jsonPath("$[2].media.title").value("Film#1"))
                .andExpect(jsonPath("$[3].media.title").value("Film#2"))
                .andExpect(jsonPath("$[4].media.title").value("Film#3"));
    }

    @Test
    public void testGetInventoryPaginationSecondPage() throws Exception {

        // Create 20 extra media in the DB first
        var library = libraryDB.getLibrary();
        for (int i = 1; i <= 20; i++) {
            library.addToInventory(
                    new Film("" + i, "Film#" + i, Rating.G, i, "SE325", Genre.Action),
                    1);
        }

        mockMvc.perform(get("/inventory?page=2&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Prev-Page"))
                .andExpect(header().longValue("Prev-Page", 1))
                .andExpect(header().exists("Next-Page"))
                .andExpect(header().longValue("Next-Page", 3))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].media.title").value("Film#4"))
                .andExpect(jsonPath("$[1].media.title").value("Film#5"))
                .andExpect(jsonPath("$[2].media.title").value("Film#6"))
                .andExpect(jsonPath("$[3].media.title").value("Film#7"))
                .andExpect(jsonPath("$[4].media.title").value("Film#8"));
    }

    @Test
    public void testGetInventoryPaginationLastPage() throws Exception {

        // Create 20 extra media in the DB first
        var library = libraryDB.getLibrary();
        for (int i = 1; i <= 20; i++) {
            library.addToInventory(
                    new Film("" + i, "Film#" + i, Rating.G, i, "SE325", Genre.Action),
                    1);
        }

        mockMvc.perform(get("/inventory?page=5&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Prev-Page"))
                .andExpect(header().longValue("Prev-Page", 4))
                .andExpect(header().doesNotExist("Next-Page"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].media.title").value("Film#19"))
                .andExpect(jsonPath("$[1].media.title").value("Film#20"));
    }

    @Test
    public void testGetInventoryPaginationPageNumberTooHigh() throws Exception {

        // Create 20 extra media in the DB first
        var library = libraryDB.getLibrary();
        for (int i = 1; i <= 20; i++) {
            library.addToInventory(
                    new Film("" + i, "Film#" + i, Rating.G, i, "SE325", Genre.Action),
                    1);
        }

        mockMvc.perform(get("/inventory?page=6&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Prev-Page"))
                .andExpect(header().doesNotExist("Next-Page"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetInventoryPaginationPageNumberTooLow() throws Exception {

        mockMvc.perform(get("/inventory?page=-1&size=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
