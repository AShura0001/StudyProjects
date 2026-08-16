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
import uoa.se325.lab03.service.LibraryDB;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InventoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryDB libraryDB;

    @Test
    public void testGetInventory() throws Exception {
        mockMvc.perform(get("/inventory")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$[0].media.mediaType").isString())
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[1].media.title").value("Black Widow"))
                .andExpect(jsonPath("$[1].media.mediaType").isString())
                .andExpect(jsonPath("$[1].count").value(1));
    }

    @Test
    public void testSearchInventory() throws Exception {
        mockMvc.perform(get("/inventory")
                        .param("search", "thousand")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$[0].count").value(3));
    }
    
    @Test
    public void testSearchNonExistentInventory() throws Exception {
        mockMvc.perform(get("/inventory")
                        .param("search", "nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    public void testGetAvailableInventory() throws Exception {

        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        // Then Black Widow should not be available.
        mockMvc.perform(get("/inventory/available")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$[0].count").value(3));
    }

    @Test
    public void testSearchAvailableInventory() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        // Search for "thousand" in available inventory
        mockMvc.perform(get("/inventory/available")
                        .param("search", "thousand")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$[0].count").value(3));
    }

    @Test
    public void testSearchUnavailableInventory() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        // Search for "black" in available inventory
        mockMvc.perform(get("/inventory/available")
                        .param("search", "black")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetSingleInventoryItemByBarcode() throws Exception {
        mockMvc.perform(get("/inventory/000000001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media.title").value("Twenty Thousand Leagues Under the Sea"))
                .andExpect(jsonPath("$.count").value(3));
    }
    
    @Test
    public void testGetNonExistingInventoryItemByBarcode() throws Exception {
        mockMvc.perform(get("/inventory/000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
