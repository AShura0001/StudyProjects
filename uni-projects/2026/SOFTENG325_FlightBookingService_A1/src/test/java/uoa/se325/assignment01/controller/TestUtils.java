package uoa.se325.assignment01.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uoa.se325.assignment01.auth.SecurityUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TestUtils {

    public static final JsonNode testData;

    static {
        // Create an ObjectMapper instance to parse the JSON
        ObjectMapper objectMapper = new ObjectMapper();

        // Read the JSON file and deserialize it into a Map
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream("db-contents.json")) {
            testData = objectMapper.readTree(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cookie logInAsAlice(MockMvc mvc) throws Exception {
        return logInAs(mvc, "Alice", "pa55word");
    }

    public static Cookie logInAsBob(MockMvc mvc) throws Exception {
        return logInAs(mvc, "Bob", "12345");
    }

    public static Cookie logInAs(MockMvc mvc, String username, String password) throws Exception {
        String loginRequest = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        Cookie cookie = mvc.perform(post("/api/users/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("authToken"))
                .andReturn().getResponse().getCookie("authToken");

        // Test JWT inside cookie
        String encodedJWT = cookie.getValue();
        DecodedJWT jwt = SecurityUtils.decodeJWT(encodedJWT);
        assertNotNull(jwt.getClaim("username").asString());
        assertNotNull(jwt.getClaim("uuid").asString());
        assertEquals(username, jwt.getClaim("username").asString());

        return cookie;
    }

    private static JsonNode getFlightData(String flightName) throws IOException {
        return testData.valueStream()
                .filter(node -> node.has("name") && node.get("name").asText().equals(flightName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    public static void checkFlight(ResultActions actions, String prefix, String flightName) throws Exception {

        JsonNode flightData = getFlightData(flightName);
        checkFlight(actions, prefix, flightData);
    }

    public static void checkFlight(ResultActions actions, String prefix, JsonNode flightData) throws Exception {
        actions
                .andExpect(jsonPath(prefix + ".id").value(flightData.get("id").asInt()))
                .andExpect(jsonPath(prefix + ".name").value(flightData.get("name").asText()))
                .andExpect(jsonPath(prefix + ".departureTime").value(Matchers.startsWith(flightData.get("departureTime").asText())))
                .andExpect(jsonPath(prefix + ".arrivalTime").value(Matchers.startsWith(flightData.get("arrivalTime").asText())))
                .andExpect(jsonPath(prefix + ".flightStatus").value(flightData.get("flightStatus").asText()));

        checkAirport(actions, prefix + ".origin", flightData.get("origin"));
        checkAirport(actions, prefix + ".destination", flightData.get("destination"));
        checkAircraftType(actions, prefix + ".aircraftType", flightData.get("aircraftType"));
    }

    public static void checkAirport(ResultActions actions, String prefix, JsonNode airportData) throws Exception {
        actions
                .andExpect(jsonPath(prefix + ".id").value(airportData.get("id").asInt()))
                .andExpect(jsonPath(prefix + ".name").value(airportData.get("name").asText()))
                .andExpect(jsonPath(prefix + ".code").value(airportData.get("code").asText()))
                .andExpect(jsonPath(prefix + ".latitude").value(airportData.get("latitude").asDouble()))
                .andExpect(jsonPath(prefix + ".longitude").value(airportData.get("longitude").asDouble()))
                .andExpect(jsonPath(prefix + ".code").value(airportData.get("code").asText()))
                .andExpect(jsonPath(prefix + ".timeZone").value(airportData.get("timeZone").asText()));
    }

    public static void checkAircraftType(ResultActions actions, String prefix, JsonNode aircraftTypeData) throws Exception {
        actions
                .andExpect(jsonPath(prefix + ".id").value(aircraftTypeData.get("id").asInt()))
                .andExpect(jsonPath(prefix + ".name").value(aircraftTypeData.get("name").asText()));
    }
}
