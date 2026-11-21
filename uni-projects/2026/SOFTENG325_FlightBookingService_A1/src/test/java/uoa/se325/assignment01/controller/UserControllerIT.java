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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests functionality related to users / authentication
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Sql("classpath:db-init.sql")
public class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests that a user is allowed to login when supplying the correct username and password. The response should be a
     * 204 No Content, with an authToken cookie.
     */
    @Test
    public void testLogin() throws Exception {
        TestUtils.logInAsAlice(mockMvc);
    }


    /**
     * Tests that logging in with a bad username returns a 401 Unauthorized error.
     */
    @Test
    public void testLoginWithBadUsername() throws Exception {
        String loginRequest = """
                {
                    "username": "BadUsername",
                    "password": "pa55word"
                }
                """;

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests that logging in with a bad password returns a 401 Unauthorized error.
     */
    @Test
    public void testLoginWithBadPassword() throws Exception {
        String loginRequest = """
                {
                    "username": "Alice",
                    "password": "notpassword"
                }
                """;

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }


    /**
     * Tests that logging out by sending a GET request to /api/users/logout returns a 204 status
     * and the authToken cookie is set to have a maxAge of 0.
     */
    @Test
    public void testLogout() throws Exception {
        TestUtils.logInAsAlice(mockMvc); // Log in as Alice to get the authToken

        mockMvc.perform(get("/api/users/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("authToken", ""))
                .andExpect(cookie().maxAge("authToken", 0));
    }


    /**
     * Tests that a new user can be successfully created by POSTing to /api/users with a valid username and password.
     */
    @Test
    public void testCreateNewUser() throws Exception {
        String createUserRequest = """
                {
                    "username": "NewUser",
                    "password": "newPassword123"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    /**
     * Tests that attempting to create a user with username "Alice" returns a 409 Conflict error
     * because the username already exists.
     */
    @Test
    public void testCreateUserWithExistingUsername() throws Exception {
        String createUserRequest = """
                {
                    "username": "Alice",
                    "password": "somePassword"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest))
                .andExpect(status().isConflict());
    }
}
