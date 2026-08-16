package uoa.se325.lab03.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uoa.se325.lab03.config.TestConfig;
import uoa.se325.lab03.model.library.Film;
import uoa.se325.lab03.model.library.Genre;
import uoa.se325.lab03.model.library.Rating;
import uoa.se325.lab03.service.LibraryDB;

import javax.imageio.ImageIO;

import java.awt.*;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InventoryControllerContentNegotiationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryDB libraryDB;

    @Test
    public void testGetInventoryCsv() throws Exception {

        String[] lines = {
                "BarCode,Type,Title,Count",
                "000000001,Book,Twenty Thousand Leagues Under the Sea,3",
                "000000002,Film,Black Widow,1"
        };

        String response = mockMvc.perform(get("/inventory")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    @Test
    public void testSearchInventoryCsv() throws Exception {

        String[] lines = {
                "BarCode,Type,Title,Count",
                "000000001,Book,Twenty Thousand Leagues Under the Sea,3",
        };

        String response = mockMvc.perform(get("/inventory")
                        .param("search", "thousand")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    @Test
    public void testSearchNonExistentInventoryCsv() throws Exception {

        String[] lines = {
                "BarCode,Type,Title,Count",
        };

        String response = mockMvc.perform(get("/inventory")
                        .param("search", "nonexistent")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    @Test
    public void testGetAvailableInventoryCsv() throws Exception {

        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        String[] lines = {
                "BarCode,Type,Title,Count",
                "000000001,Book,Twenty Thousand Leagues Under the Sea,3",
        };

        // Then Black Widow should not be available.
        String response = mockMvc.perform(get("/inventory/available")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    @Test
    public void testSearchAvailableInventoryCsv() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        String[] lines = {
                "BarCode,Type,Title,Count",
                "000000001,Book,Twenty Thousand Leagues Under the Sea,3",
        };

        // Search for "thousand" in available inventory
        String response = mockMvc.perform(get("/inventory/available")
                        .param("search", "thousand")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    @Test
    public void testSearchUnavailableInventoryCsv() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        String[] lines = {
                "BarCode,Type,Title,Count",
        };

        // Search for "black" in available inventory
        String response = mockMvc.perform(get("/inventory/available")
                        .param("search", "black")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testGetInventoryPaginationSecondPageCsv() throws Exception {

        // Create 20 extra media in the DB first
        var library = libraryDB.getLibrary();
        for (int i = 1; i <= 20; i++) {
            library.addToInventory(
                    new Film("" + i, "Film#" + i, Rating.G, i, "SE325", Genre.Action),
                    1);
        }

        String[] lines = {
                "BarCode,Type,Title,Count",
                "4,Film,Film#4,1",
                "5,Film,Film#5,1",
                "6,Film,Film#6,1",
                "7,Film,Film#7,1",
                "8,Film,Film#8,1",
        };

        String response = mockMvc.perform(get("/inventory?page=2&size=5")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Prev-Page"))
                .andExpect(header().longValue("Prev-Page", 1))
                .andExpect(header().exists("Next-Page"))
                .andExpect(header().longValue("Next-Page", 3))
                .andReturn().getResponse().getContentAsString();

        verifyCsvResponse(lines, response);
    }

    private void verifyCsvResponse(String[] expected, String response) {
        String[] actual = response.trim().split("\n");
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            String formattedActual = actual[i].replaceAll("\"", "").trim();
            assertEquals(expected[i].trim(), formattedActual);
        }
    }

    @Test
    public void testGetInventoryCoverImage() throws Exception {
        byte[] bytes = mockMvc.perform(get("/inventory/000000002/cover-image")
                        .accept("image/png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn().getResponse().getContentAsByteArray(); // Assuming a helper method to get the image bytes

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            var image = ImageIO.read(in);
            assertEquals(250, image.getWidth());
            assertEquals(371, image.getHeight());
        }
    }

    @Test
    public void testGetNonexistentInventoryItemCoverImage() throws Exception {
        mockMvc.perform(get("/inventory/000000999/cover-image")
                        .accept("image/png"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetNonexistentImage() throws Exception {

        // Set black widow's image to null
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        blackWidow.setCoverImage(null);

        mockMvc.perform(get("/inventory/000000002/cover-image")
                        .accept("image/png"))
                .andExpect(status().isNotFound());
    }
}
