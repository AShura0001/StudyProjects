package uoa.se325.lab04.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ConcertControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ConcertTestUtils testUtils;

    @BeforeEach
    public void setUp() {
        testUtils.setUp();
    }

    /**
     * This test will always pass, assuming Spring loads correctly and the setUp() method above
     * works as expected.
     */
    @Test
    public void testContextLoads() {
    }

    /**
     * Tests that we can get a single concert from the web service, given its id.
     * <p>
     * The request should return a 200 response, with the requested concert. The concert should contain all
     * performers and dates for that concert.
     */
    @Test
    public void testGetSingleConcert() throws Exception {
        mvc.perform(get("/concerts/{id}", testUtils.ptxWorldTourId)
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUtils.ptxWorldTourId))
                .andExpect(jsonPath("$.title").value("PTX: The World Tour"))
                .andExpect(jsonPath("$.dates[0]").value("2020-02-15T20:00:00"))
                .andExpect(jsonPath("$.performers[0].name").value("Pentatonix"))
                .andExpect(jsonPath("$.performers[0].genre").value("Acappella"))
                .andExpect(jsonPath("$.performers[0].performerType").value("Band"))
                .andExpect(jsonPath("$.performers[0].members.length()").value(5))
                .andExpect(jsonPath("$.performers[0].members[0].name").value("Scott Hoying"))
                .andExpect(jsonPath("$.performers[0].members[0].genre").value("Acappella"))
                .andExpect(jsonPath("$.performers[0].members[0].performerType").value("IndividualArtist"))
                .andExpect(jsonPath("$.performers[0].members[4].name").value("Matt Sallee"));
    }


    /**
     * A more advanced version of the test above. Makes sure the web service still functions correctly when requesting
     * a concert with multiple performers and dates.
     */
    @Test
    public void testGetSingleConcertWithMultiplePerformersAndDates() throws Exception {
        mvc.perform(get("/concerts/{id}", testUtils.hjShowId)
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUtils.hjShowId))
                .andExpect(jsonPath("$.title").value("Hugh Jackman: The Man. The Music. The Show."))
                .andExpect(jsonPath("$.dates[0]").value("2019-09-06T20:00:00"))
                .andExpect(jsonPath("$.dates[1]").value("2019-09-07T20:00:00"))
                .andExpect(jsonPath("$.performers[0].name").value("Hugh Jackman"))
                .andExpect(jsonPath("$.performers[0].genre").value("Theatre"))
                .andExpect(jsonPath("$.performers[0].performerType").value("IndividualArtist"))
                .andExpect(jsonPath("$.performers[1].name").value("Keala Settle"))
                .andExpect(jsonPath("$.performers[1].genre").value("Theatre"))
                .andExpect(jsonPath("$.performers[1].performerType").value("IndividualArtist"));
    }


    /**
     * Tests that requesting a nonexistent concert returns a 404 response.
     */
    @Test
    public void testGetNonexistentConcertShouldReturn404() throws Exception {
        mvc.perform(get("/concerts/{id}", 9999)
                        .accept("application/json"))
                .andExpect(status().isNotFound());
    }


    /**
     * Tests fetching all concerts with a GET request to /concerts.
     * For each concert, verifies the title and the size of performers and dates arrays.
     */
    @Test
    public void testGetAllConcerts() throws Exception {
        mvc.perform(get("/concerts")
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Assuming there are two concerts in the setup
                .andExpect(jsonPath("$[0].title").value("PTX: The World Tour"))
                .andExpect(jsonPath("$[0].dates.length()").value(1))
                .andExpect(jsonPath("$[0].performers.length()").value(1))
                .andExpect(jsonPath("$[1].title").value("Hugh Jackman: The Man. The Music. The Show."))
                .andExpect(jsonPath("$[1].dates.length()").value(2))
                .andExpect(jsonPath("$[1].performers.length()").value(2));
    }
}
