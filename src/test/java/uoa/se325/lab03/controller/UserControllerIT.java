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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryDB libraryDB;


    @Test
    public void testGetUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libraryCardNumber").value("12345"))
                .andExpect(jsonPath("$[0].name").value("Joe Bloggs"))
                .andExpect(jsonPath("$[0].dateOfBirth").value("1986-03-17"))
                .andExpect(jsonPath("$[0].dateJoined").value("2015-01-01"))
                .andExpect(jsonPath("$[0].borrowedMedia").isEmpty());
    }

    @Test
    public void testGetUserByLibraryCardNumber() throws Exception {
        mockMvc.perform(get("/users/12345") // Assuming this is the endpoint to get a user by library card number
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardNumber").value("12345"))
                .andExpect(jsonPath("$.name").value("Joe Bloggs"))
                .andExpect(jsonPath("$.dateOfBirth").value("1986-03-17"))
                .andExpect(jsonPath("$.dateJoined").value("2015-01-01"))
                .andExpect(jsonPath("$.borrowedMedia").isEmpty());
    }

    @Test
    public void testGetNonExistentUser() throws Exception {
        mockMvc.perform(get("/users/99999") // Assuming 99999 does not exist in the test data
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetUserBorrowedMedia() throws Exception {

        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        mockMvc.perform(get("/users/12345/borrowed-media")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].barCode").value("000000002"))
                .andExpect(jsonPath("$[0].title").value("Black Widow"));
    }

    @Test
    public void testGetUserBorrowedMediaWhenEmpty() throws Exception {
        mockMvc.perform(get("/users/12345/borrowed-media")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }


    @Test
    public void testBorrowMediaWithPatchRequest() throws Exception {
        mockMvc.perform(patch("/users/12345/borrowed-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "barCode": "000000002"
                                }
                                """))
                .andExpect(status().isNoContent());

        // Check libraryDB
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        assertTrue(joeBloggs.getBorrowedMedia().contains(blackWidow));
    }

    @Test
    public void testBorrowMediaForNonExistentUser() throws Exception {
        mockMvc.perform(patch("/users/99999/borrowed-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "barCode": "000000002"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBorrowNonExistentMedia() throws Exception {
        mockMvc.perform(patch("/users/12345/borrowed-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "barCode": "999999999"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBorrowAlreadyBorrowedMedia() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        // Attempt to borrow the same media again
        mockMvc.perform(patch("/users/12345/borrowed-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "barCode": "000000002"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testReturnBorrowedMediaWithDeleteRequest() throws Exception {
        // Borrow Black Widow first
        var joeBloggs = libraryDB.getLibrary().getUsers().get(0);
        var blackWidow = libraryDB.getLibrary().getInventory().get(1).getMedia();
        libraryDB.getLibrary().lendMediaTo(joeBloggs, blackWidow);

        // Return the borrowed media
        mockMvc.perform(delete("/users/12345/borrowed-media/000000002"))
                .andExpect(status().isNoContent());

        // Check libraryDB to ensure the media was returned
        assertTrue(joeBloggs.getBorrowedMedia().isEmpty());
    }
}
