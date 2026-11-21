package uoa.se325.assignment01.controller;

import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests functionality related to making, retrieving, and cancelling individual flight bookings.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Sql("classpath:db-init.sql")
public class BookingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests that an authenticated user can make a booking on a flight.
     */
    @Test
    public void testMakeBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        testMakeBooking(authCookie, 43, "1A", "59A", "3K");
    }

    /**
     * Tests that an unauthenticated user can't make a booking. A 401 Unauthorized response should be returned.
     */
    @Test
    public void testMakeBookingNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBookingRequestJson(43, "1A", "59A", "3K")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that an authenticated user can't make a booking for invalid seats. A 404 or 409 should be returned
     * (either is ok).
     */
    @Test
    public void testMakeBookingWithInvalidSeats() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        mockMvc.perform(post("/api/bookings")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBookingRequestJson(43, "999A")))
                .andExpect(status().is(Matchers.oneOf(409, 404)));
    }

    /**
     * Tests that a booking made by an authenticated user can be retrieved. The retrieved booking should contain the
     * booked seat code info, ordered by row then by letter.
     */
    @Test
    public void testRetrieveBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(get(bookingUri.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedSeats.length()").value(3))
                .andExpect(jsonPath("$.bookedSeats[0]").value("1A"))
                .andExpect(jsonPath("$.bookedSeats[1]").value("3K"))
                .andExpect(jsonPath("$.bookedSeats[2]").value("59A"))
                .andExpect(jsonPath("$.totalCost").value(7150))
                .andExpect(jsonPath("$.flight.name").value("YJY-087"))
                .andExpect(jsonPath("$.flight.origin.code").value("SIN"));
    }

    /**
     * Tests that a request by an authenticated user for a booking with a nonexistent id will return a 404 response.
     */
    @Test
    public void testRetrieveNonexistentBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);

        mockMvc.perform(get("/api/bookings/1")
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that a request by an unauthenticated user to retrieve a booking will return a 401 response.
     */
    @Test
    public void testRetrieveBookingWhileNotAuthenticated() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(get(bookingUri.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that a request by an authenticated user for a booking other than their own booking will return a
     * 404 response.
     */
    @Test
    public void testRetrieveOtherUsersBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        authCookie = TestUtils.logInAsBob(mockMvc);

        mockMvc.perform(get(bookingUri.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that we can get the bookings for an authenticated user with no current bookings. In this case, a 200 OK
     * response should be returned, with an empty booking list.
     */
    @Test
    public void testRetrieveEmptyBookingList() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);

        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Tests that an authenticated user correctly gets a list with a single booking if they've only made one booking
     */
    @Test
    public void testRetrieveBookingListWithSingleBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookedSeats.length()").value(3))
                .andExpect(jsonPath("$[0].bookedSeats[0]").value("1A"))
                .andExpect(jsonPath("$[0].bookedSeats[1]").value("3K"))
                .andExpect(jsonPath("$[0].bookedSeats[2]").value("59A"))
                .andExpect(jsonPath("$[0].totalCost").value(7150))
                .andExpect(jsonPath("$[0].flight.name").value("YJY-087"))
                .andExpect(jsonPath("$[0].flight.origin.code").value("SIN"));
    }

    /**
     * Tests that an authenticated user correctly gets a list with multiple bookings, in the correct order (ordered
     * by departure time, ascending).
     */
    @Test
    public void testRetrieveBookingListWithMultipleBookings() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        testMakeBooking(authCookie, 43, "1A", "59A", "3K");
        testMakeBooking(authCookie, 37, "52E", "41K", "31J");

        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bookedSeats.length()").value(3))
                .andExpect(jsonPath("$[0].bookedSeats[0]").value("31J"))
                .andExpect(jsonPath("$[0].bookedSeats[1]").value("41K"))
                .andExpect(jsonPath("$[0].bookedSeats[2]").value("52E"))
                .andExpect(jsonPath("$[0].totalCost").value(1074))
                .andExpect(jsonPath("$[0].flight.name").value("DPX-900"))
                .andExpect(jsonPath("$[1].bookedSeats.length()").value(3))
                .andExpect(jsonPath("$[1].bookedSeats[0]").value("1A"))
                .andExpect(jsonPath("$[1].bookedSeats[1]").value("3K"))
                .andExpect(jsonPath("$[1].bookedSeats[2]").value("59A"))
                .andExpect(jsonPath("$[1].totalCost").value(7150))
                .andExpect(jsonPath("$[1].flight.name").value("YJY-087"))
                .andExpect(jsonPath("$[1].flight.origin.code").value("SIN"));
    }

    /**
     * Tests that only the currently authenticated user's bookings will be returned in the booking list.
     */
    @Test
    public void testRetrieveBookingListWithOtherUsersHavingBookigns() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        authCookie = TestUtils.logInAsBob(mockMvc);
        testMakeBooking(authCookie, 37, "52E", "41K", "31J");

        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookedSeats.length()").value(3))
                .andExpect(jsonPath("$[0].bookedSeats[0]").value("31J"))
                .andExpect(jsonPath("$[0].bookedSeats[1]").value("41K"))
                .andExpect(jsonPath("$[0].bookedSeats[2]").value("52E"))
                .andExpect(jsonPath("$[0].totalCost").value(1074))
                .andExpect(jsonPath("$[0].flight.name").value("DPX-900"));
    }

    /**
     * Tests that we can't get bookings when a user is not authenticated. In this case, a 401 Unauthorized response
     * should be returned.
     */
    @Test
    public void testRetrieveBookingListWhileNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that a booking can be cancelled by the user who booked it. A 204 response should be returned. Also makes
     * sure that the booking is actually deleted from the system.
     */
    @Test
    public void testCancelBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(delete(bookingUri.toString())
                        .cookie(authCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(bookingUri.toString())
                        .cookie(authCookie))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that we can't cancel someone else's booking. We should still receive a 204 No Content response,
     * but the booking won't be deleted from the database.
     */
    @Test
    public void testCancelAnotherUsersBooking() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        authCookie = TestUtils.logInAsBob(mockMvc);

        mockMvc.perform(delete(bookingUri.toString())
                        .cookie(authCookie))
                .andExpect(status().isNoContent());

        authCookie = TestUtils.logInAsAlice(mockMvc);

        mockMvc.perform(get(bookingUri.toString())
                        .cookie(authCookie))
                .andExpect(status().isOk());
    }

    /**
     * Tests that we can't cancel bookings while not authenticated. A 401 should be returned.
     */
    @Test
    public void testCancelBookingWhileNotAuthenticated() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        URI bookingUri = testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(delete(bookingUri.toString()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that getting a flight's booking info after a booking is made, will show that booking's seats as taken.
     */
    @Test
    public void testGetFlightBookingInfoAfterBookingMade() throws Exception {
        Cookie authCookie = TestUtils.logInAsAlice(mockMvc);
        testMakeBooking(authCookie, 43, "1A", "59A", "3K");

        mockMvc.perform(get("/api/flights/43/booking-info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedSeats.length()").value(3))
                .andExpect(jsonPath("$.bookedSeats").value(Matchers.containsInAnyOrder("1A", "59A", "3K")));
    }

    /**
     * Tests that we can make a booking with the given authentication information to the given flight for
     * the given seats.
     *
     * @param authCookie the auth details
     * @param flightId   the id of the flight to book
     * @param seats      the codes of the seats to book on that flight
     */
    private URI testMakeBooking(Cookie authCookie, long flightId, String... seats) throws Exception {
        var locationHeader = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBookingRequestJson(flightId, seats))
                        .cookie(authCookie))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", Matchers.containsString("/api/bookings/")))
                .andReturn().getResponse().getHeader("Location");

        assert locationHeader != null;
        return URI.create(locationHeader);
    }

    /**
     * Gets a JSON string for a booking request for the given flight and seats
     *
     * @param flightId the id of the flight to book
     * @param seats    the codes of the seats to book on that flight
     * @return a JSON string
     */
    private static String getBookingRequestJson(long flightId, String... seats) {
        return """
                {
                    "flightId": %s,
                    "requestedSeats": [%s]
                }
                """.formatted(
                flightId,
                String.join(", ", Arrays.stream(seats).map(s -> "\"" + s + "\"").toList())
        );
    }
}
