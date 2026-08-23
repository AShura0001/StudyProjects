package uoa.se325.lab04.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PerformerControllerIT {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ConcertTestUtils testUtils;

    @BeforeEach
    public void setUp() {
        testUtils.setUp();
    }

    /**
     * Tests that a 200 response is returned, along with the correct performer info, when requesting a band with
     * a given id.
     */
    @Test
    public void testGetSingleBandById() throws Exception {
        // Perform GET request to /performers/{id} for Pentatonix
        mvc.perform(get("/performers/" + testUtils.ptxId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUtils.ptxId))
                .andExpect(jsonPath("$.name").value("Pentatonix"))
                .andExpect(jsonPath("$.genre").value("Acappella"))
                .andExpect(jsonPath("$.performerType").value("Band"))
                .andExpect(jsonPath("$.members.length()").value(5))
                .andExpect(jsonPath("$.members[0].name").value("Scott Hoying"))
                .andExpect(jsonPath("$.members[0].genre").value("Acappella"))
                .andExpect(jsonPath("$.members[0].performerType").value("IndividualArtist"))
                .andExpect(jsonPath("$.members[1].name").value("Mitch Grassi"))
                .andExpect(jsonPath("$.members[2].name").value("Kirstin Maldonado"))
                .andExpect(jsonPath("$.members[3].name").value("Kevin Olusola"))
                .andExpect(jsonPath("$.members[4].name").value("Matt Sallee"));
    }

    /**
     * Tests that a 200 response is returned, along with the correct performer info, when requesting an individual artist
     * with a given id.
     */
    @Test
    public void testGetSingleIndividualArtistById() throws Exception {
        // Perform GET request to /performers/{id} for Hugh Jackman
        mvc.perform(get("/performers/" + testUtils.hughJackmanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUtils.hughJackmanId))
                .andExpect(jsonPath("$.name").value("Hugh Jackman"))
                .andExpect(jsonPath("$.genre").value("Theatre"))
                .andExpect(jsonPath("$.dateOfBirth").value("1968-10-12"))
                .andExpect(jsonPath("$.performerType").value("IndividualArtist"));
    }


    /**
     * Tests that a 404 response is returned when requesting a performer with a nonexistent id.
     */
    @Test
    public void testGetNonexistentPerformerById() throws Exception {
        // Perform GET request to /performers/{id} with an invalid id
        mvc.perform(get("/performers/{id}", 999999))
                .andExpect(status().isNotFound());
    }


    /**
     * Tests that a 200 response is returned and all performers are included when requesting all performers.
     * <p>
     * Should return PTX and each of its band members as separate array entries, along with Hugh and Keala.
     */
    @Test
    public void testGetAllPerformers() throws Exception {
        // Perform GET request to /performers
        mvc.perform(get("/performers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[*].name").value(
                        hasItems("Pentatonix", "Hugh Jackman", "Keala Settle", "Scott Hoying",
                                "Mitch Grassi", "Kirstin Maldonado", "Kevin Olusola", "Matt Sallee")));
    }
}
