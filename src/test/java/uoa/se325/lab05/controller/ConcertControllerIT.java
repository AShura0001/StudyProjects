package uoa.se325.lab05.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uoa.se325.lab05.model.Concert;
import uoa.se325.lab05.repository.ConcertRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ConcertControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ConcertTestUtils testUtils;

    private ConcertTestUtils.Ids ids;
    @Autowired
    private ConcertRepository concertRepository;

    @BeforeEach
    public void setUp() {
        this.ids = testUtils.setUp();
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

        mvc.perform(get("/concerts/{id}", ids.ptxWorldTourId)
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ids.ptxWorldTourId))
                .andExpect(jsonPath("$.title").value("PTX: The World Tour"))
                .andExpect(jsonPath("$.durationInMinutes").value(240))
                .andExpect(jsonPath("$.standardTicketPrice").value(150))
                .andExpect(jsonPath("$.startTime").value("2020-02-15T20:00:00"))
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

        System.out.println("INTEST");
        System.out.println(ids.hjShowId);

        mvc.perform(get("/concerts/{id}", ids.hjShowId)
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ids.hjShowId))
                .andExpect(jsonPath("$.title").value("Hugh Jackman: The Man. The Music. The Show."))
                .andExpect(jsonPath("$.startTime").value("2019-09-06T20:00:00"))
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
                .andExpect(jsonPath("$[0].performers.length()").value(1))
                .andExpect(jsonPath("$[1].title").value("Hugh Jackman: The Man. The Music. The Show."))
                .andExpect(jsonPath("$[1].performers.length()").value(2));
    }

    /**
     * Tests that we can create a new concert
     */
    @Test
    public void testCreateConcert() throws Exception {
        String newConcertJson = """
                {
                    "title": "New Concert",
                    "durationInMinutes": 120,
                    "standardTicketPrice": 100,
                    "startTime": "2025-08-20T20:00:00"
                }
                """;

        var location = mvc.perform(post("/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newConcertJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", Matchers.startsWith("/concerts")))
                .andReturn().getResponse().getHeader("Location");

        assertNotNull(location);
        long newConcertId = Long.parseLong(location.substring(location.lastIndexOf("/") + 1));

        Concert fromDb = concertRepository.findById(newConcertId).orElse(null);
        assertNotNull(fromDb);
        assertEquals("New Concert", fromDb.getTitle());
    }

    /**
     * Tests that we can't create a new concert if there are any concerts with
     * an overlapping date compared to this one. A 409 Conflict response should
     * be returned.
     */
    @Test
    public void testCreateConcertWithOverlappingDateBefore() throws Exception {

        // This is a date / time of the Hugh Jackman concert
        // 2019-09-06T20:00:00

        // This concert's start time is one hour earlier than HJ, but
        // its duration is two hours so it overlaps. Should not be able
        // to add this concert.
        String newConcertJson = """
                {
                    "title": "New Concert",
                    "durationInMinutes": 120,
                    "standardTicketPrice": 100,
                    "startTime": "2019-09-06T19:00:00"
                }
                """;

        mvc.perform(post("/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newConcertJson))
                .andExpect(status().isConflict());
    }

    /**
     * Tests that we can't create a new concert if there are any concerts with
     * an overlapping date compared to this one. A 409 Conflict response should
     * be returned.
     */
    @Test
    public void testCreateConcertWithOverlappingDateAfter() throws Exception {

        // This is a date / time of the Hugh Jackman concert
        // 2019-09-06T20:00:00
        // The duration is 4 hours (the default).

        // This concert's start time is three hours after HJ starts, but
        // HJ won't have finished yet, so we shouldn't be able to add this
        // concert.
        String newConcertJson = """
                {
                    "title": "New Concert",
                    "durationInMinutes": 120,
                    "standardTicketPrice": 100,
                    "startTime": "2019-09-06T23:00:00"
                }
                """;

        mvc.perform(post("/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newConcertJson))
                .andExpect(status().isConflict());
    }

    @Test
    public void testSetConcertSpecialPrice() throws Exception {
        mvc.perform(put("/concerts/{id}/special-ticket-price", ids.ptxWorldTourId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("50"))
                .andExpect(status().isNoContent());

        Concert fromDb = concertRepository.findById(ids.ptxWorldTourId).orElse(null);
        assertNotNull(fromDb);
        assertEquals(50, fromDb.getSpecialTicketPrice());
    }
}
