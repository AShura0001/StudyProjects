package uoa.se325.assignment01.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests functionality related to flight searches, and searching for booking info for existing flights
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Sql("classpath:db-init.sql")
public class FlightControllerIT {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests that a flight is returned when querying by id.
     */
    @Test
    public void testGetFlightById() throws Exception {
        var actions = mockMvc.perform(get("/api/flights/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        TestUtils.checkFlight(actions, "$", "ZNJ-242");
    }

    /**
     * Tests that a 404 is returned when querying by id for a nonexistent flight.
     */
    @Test
    public void testGetNonexistentFlightById() throws Exception {
        var actions = mockMvc.perform(get("/api/flights/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that the correct flight info is returned when we search for flights by airport name. The
     * response should be a 200 OK with a list of valid flights as JSON. The list should
     * contain 4 flights, sorted by departure time ascending.
     */
    @Test
    public void testFlightSearchAirportsByName() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=Auckland&destination=Sydney")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        TestUtils.checkFlight(actions, "$[0]", "ZNJ-242");
        TestUtils.checkFlight(actions, "$[1]", "WJF-883");
        TestUtils.checkFlight(actions, "$[2]", "ZWZ-576");
        TestUtils.checkFlight(actions, "$[3]", "YLJ-355");
    }

    /**
     * Tests that searching for airports by name is case-insensitive. The same airports
     * as above should be returned even though the casing in the search terms is different.
     */
    @Test
    public void testFlightSearchAirportsByNameCaseInsensitive() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=AUCKland&destination=SyDnEy")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        TestUtils.checkFlight(actions, "$[0]", "ZNJ-242");
        TestUtils.checkFlight(actions, "$[1]", "WJF-883");
        TestUtils.checkFlight(actions, "$[2]", "ZWZ-576");
        TestUtils.checkFlight(actions, "$[3]", "YLJ-355");
    }

    /**
     * Tests that the correct flight info is returned when we search for flights by airport code. The
     * response should be a 200 OK with a list of valid flights as JSON. The list should
     * contain 4 flights, sorted by departure time ascending.
     */
    @Test
    public void testFlightSearchAirportsByCode() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=AKL&destination=SYD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        TestUtils.checkFlight(actions, "$[0]", "ZNJ-242");
        TestUtils.checkFlight(actions, "$[1]", "WJF-883");
        TestUtils.checkFlight(actions, "$[2]", "ZWZ-576");
        TestUtils.checkFlight(actions, "$[3]", "YLJ-355");
    }

    /**
     * Tests that searching for airports by code is case-insensitive. The same airports
     * as above should be returned even though the casing in the search terms is different.
     */
    @Test
    public void testFlightSearchAirportsByCodeCaseInsensitive() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=akl&destination=syD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        TestUtils.checkFlight(actions, "$[0]", "ZNJ-242");
        TestUtils.checkFlight(actions, "$[1]", "WJF-883");
        TestUtils.checkFlight(actions, "$[2]", "ZWZ-576");
        TestUtils.checkFlight(actions, "$[3]", "YLJ-355");
    }

    /**
     * Tests that a flight search can get results from multiple matching origins, in the correct order.
     * "ng" matches los aNGeles and siNGapore. This search should return 8 results.
     */
    @Test
    public void testFlightSearchMultipleMatchingOrigins() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=ng&destination=SYD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));

        TestUtils.checkFlight(actions, "$[0]", "AWJ-994");
        TestUtils.checkFlight(actions, "$[1]", "SJY-964");
        TestUtils.checkFlight(actions, "$[2]", "MTU-228");
        TestUtils.checkFlight(actions, "$[3]", "ZPV-405");
        TestUtils.checkFlight(actions, "$[4]", "VBR-241");
        TestUtils.checkFlight(actions, "$[5]", "FPZ-912");
        TestUtils.checkFlight(actions, "$[6]", "FHF-294");
        TestUtils.checkFlight(actions, "$[7]", "YAJ-395");
    }

    /**
     * Tests that a flight search can get results from multiple matching destinations, in the correct order.
     * "ng" matches los aNGeles and siNGapore. This search should return 8 results.
     */
    @Test
    public void testFlightSearchMultipleMatchingDestinations() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=AKL&destination=ng")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));

        TestUtils.checkFlight(actions, "$[0]", "ZZO-347");
        TestUtils.checkFlight(actions, "$[1]", "IEE-697");
        TestUtils.checkFlight(actions, "$[2]", "FEX-930");
        TestUtils.checkFlight(actions, "$[3]", "PWK-304");
        TestUtils.checkFlight(actions, "$[4]", "DDE-510");
        TestUtils.checkFlight(actions, "$[5]", "JQB-702");
        TestUtils.checkFlight(actions, "$[6]", "YFJ-842");
        TestUtils.checkFlight(actions, "$[7]", "VRT-298");
    }

    /**
     * Tests that a flight search with no results still returns a 200 OK response, just with an empty list.
     */
    @Test
    public void testFlightSearchNoResults() throws Exception {
        mockMvc.perform(get("/api/flights?origin=foobar&destination=AKL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Tests that a flight search with a missing origin will return a 400 response
     */
    @Test
    public void testFlightSearchMissingOrigin() throws Exception {
        mockMvc.perform(get("/api/flights?destination=AKL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that a flight search with a missing destination will return a 400 response
     */
    @Test
    public void testFlightSearchMissingDestination() throws Exception {
        mockMvc.perform(get("/api/flights?origin=AKL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that a flight search with a departure date query will return results within the specified
     * number of days of the given date, in the origin's time zone, in departure date order.
     */
    @Test
    public void testFlightSearchWithDepartureTime() throws Exception {
        var actions = mockMvc.perform(get("/api/flights?origin=AKL&destination=SYD&departureDate=2025-08-21&dayRange=8")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        TestUtils.checkFlight(actions, "$[0]", "WJF-883");
        TestUtils.checkFlight(actions, "$[1]", "ZWZ-576");
    }

    /**
     * Tests that a flight search with an invalid departure date query will return a 400 Bad Request error.
     */
    @Test
    public void testFlightSearchWitInvalidDate() throws Exception {
        mockMvc.perform(get("/api/flights?origin=AKL&destination=SYD&departureDate=2025-08-21&dayRange=-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that a flight search with an invalid day range query will return a 400 Bad Request error.
     */
    @Test
    public void testFlightSearchWitInvalidDayRange() throws Exception {
        mockMvc.perform(get("/api/flights?origin=AKL&destination=SYD&departureDate=invalid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that we can get booking info for a flight which exists
     */
    @Test
    public void testRetrieveBookingInfo() throws Exception {
        mockMvc.perform(get("/api/flights/43/booking-info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraftType.name").value("787-9 Dreamliner"))
                .andExpect(jsonPath("$.bookedSeats.length()").value(0))
                .andExpect(jsonPath("$.pricingInfo.Economy").value(550))
                .andExpect(jsonPath("$.pricingInfo.Premium").value(1650))
                .andExpect(jsonPath("$.pricingInfo.Business").value(3300))
                .andExpect(jsonPath("$.pricingInfo.FirstClass").doesNotExist());
    }

    /**
     * Tests that we get a 404 error for requesting booking info for a nonexistent flight
     */
    @Test
    public void testRetrieveBookingInfoNotFound() throws Exception {
        mockMvc.perform(get("/api/flights/9999/booking-info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
