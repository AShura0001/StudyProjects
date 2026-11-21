package uoa.se325.lab03.model.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryUnitTests {

    private User joeBloggs;
    private Media twentyThousandLeagues;
    private Media blackWidow;
    private Library library;

    private static BufferedImage blackWidowImage;
    private static BufferedImage twentyThousandLeaguesImage;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void loadImages() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (var in = classLoader.getResourceAsStream("black-widow.png")) {
            blackWidowImage = ImageIO.read(in);
        }
        try (var in = classLoader.getResourceAsStream("twenty-thousand-leagues.png")) {
            twentyThousandLeaguesImage = ImageIO.read(in);
        }

        objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void setUp() {
        joeBloggs = new User("12345", LocalDate.of(1986, 3, 17),
                LocalDate.of(2015, 1, 1), "Joe Bloggs");

        twentyThousandLeagues = new Book("000000001", "Twenty Thousand Leagues Under the Sea", 364, "Jules Verne", "Pierre-Jules Hetzel", Genre.Adventure, Genre.SciFi);
        twentyThousandLeagues.setCoverImage(twentyThousandLeaguesImage);

        blackWidow = new Film("000000002", "Black Widow", Rating.M, 134, "Cate Shortland", Genre.Action, Genre.Adventure, Genre.SciFi);
        blackWidow.setCoverImage(blackWidowImage);

        library = new Library("SE325 Library");
        library.addUser(joeBloggs);
        library.addToInventory(twentyThousandLeagues, 3);
        library.addToInventory(blackWidow, 1);
    }

    /**
     * Tests that the values in the setup function are initialized correctly
     */
    @Test
    public void testSetup() {
        assertEquals(3, library.getInventoryCount(twentyThousandLeagues));
        assertEquals(1, library.getInventoryCount(blackWidow));
        assertEquals(1, library.getUsers().size());
        assertSame(joeBloggs, library.getUsers().get(0));
        assertEquals(0, joeBloggs.getBorrowedMedia().size());
        assertEquals(2, library.getInventory().size());
        assertSame(twentyThousandLeagues, library.getInventory().get(0).getMedia());
        assertSame(blackWidow, library.getInventory().get(1).getMedia());
    }

    /**
     * Tests that a user can borrow media
     */
    @Test
    public void testBorrow() {
        library.lendMediaTo(joeBloggs, twentyThousandLeagues);
        assertEquals(2, library.getInventoryCount(twentyThousandLeagues));
        assertEquals(1, joeBloggs.getBorrowedMedia().size());
        assertSame(twentyThousandLeagues, joeBloggs.getBorrowedMedia().get(0));
    }

    /**
     * Tests that a user can return media
     */
    @Test
    public void testReturn() {
        library.lendMediaTo(joeBloggs, twentyThousandLeagues);
        library.returnMediaFrom(joeBloggs, twentyThousandLeagues);
        assertEquals(3, library.getInventoryCount(twentyThousandLeagues));
        assertEquals(0, joeBloggs.getBorrowedMedia().size());
    }

    /**
     * Tests that a user can borrow the last copy of media
     */
    @Test
    public void testBorrowLastCopy() {
        library.lendMediaTo(joeBloggs, blackWidow);
        assertEquals(0, library.getInventoryCount(blackWidow));
        assertEquals(1, joeBloggs.getBorrowedMedia().size());
        assertSame(blackWidow, joeBloggs.getBorrowedMedia().get(0));
    }

    /**
     * Tests that a user can return the last copy of media
     */
    @Test
    public void testReturnLastCopy() {
        library.lendMediaTo(joeBloggs, blackWidow);
        library.returnMediaFrom(joeBloggs, blackWidow);
        assertEquals(1, library.getInventoryCount(blackWidow));
        assertEquals(0, joeBloggs.getBorrowedMedia().size());
    }

    /**
     * Tests that users aren't allowed to return media they never borrowed in the first place
     */
    @Test
    public void testReturnUnborrowedMedia() {
        try {
            library.returnMediaFrom(joeBloggs, blackWidow);
            fail("Shouldn't let Joe return a film he never borrowed in the first place");
        } catch (LibraryException e) {
            assertEquals(1, library.getInventoryCount(blackWidow));
        }
    }

    /**
     * Tests that users aren't allowed to borrow media when all of that media has already been borrowed
     */
    @Test
    public void testBorrowTooManyCopies() {
        library.lendMediaTo(joeBloggs, blackWidow);
        try {
            library.lendMediaTo(joeBloggs, blackWidow);
            fail("Shouldn't allow Joe to borrow Black Widow as the last copy is already out");
        } catch (LibraryException e) {
            assertEquals(0, library.getInventoryCount(blackWidow));
            assertEquals(1, joeBloggs.getBorrowedMedia().size());
            assertEquals(blackWidow, joeBloggs.getBorrowedMedia().get(0));
        }
    }

    /**
     * Tests that users can't borrow media which was never in the library to begin with
     */
    @Test
    public void testBorrowNonexistentMedia() {
        Media nonexistent = new Book("blah", "blah", 1, "blah", "blah", Genre.Unknown);
        try {
            library.lendMediaTo(joeBloggs, nonexistent);
            fail("Shouldn't let Joe borrow media that's not in the library");
        } catch (LibraryException e) {
            assertEquals(0, joeBloggs.getBorrowedMedia().size());
        }
    }

    /**
     * Tests that there's no exception when serializing to JSON
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSerializeToJson() throws JsonProcessingException {
        library.lendMediaTo(joeBloggs, twentyThousandLeagues);
        library.lendMediaTo(joeBloggs, blackWidow);

        String json = objectMapper.writeValueAsString(library);
        assertEquals("Joe Bloggs", JsonPath.read(json, "$.users[0].name"));
        assertEquals("Twenty Thousand Leagues Under the Sea", JsonPath.read(json, "$.inventory[0].media.title"));
        assertEquals(2, (int) JsonPath.read(json, "$.inventory[0].count"));
        assertEquals("Black Widow", JsonPath.read(json, "$.inventory[1].media.title"));
        assertEquals(0, (int) JsonPath.read(json, "$.inventory[1].count"));
        assertEquals(twentyThousandLeagues.getBarCode(), JsonPath.read(json, "$.users[0].borrowedMedia[0]"));
        assertEquals(blackWidow.getBarCode(), JsonPath.read(json, "$.users[0].borrowedMedia[1]"));
    }

    /**
     * Tests that there's no error when deserializing from JSON, and that the deserialized data makes sense.
     * <p>
     * This test doesn't check for the expected singleton-ness of Media instances - that is tested in the next test.
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testDeserializeFromJson() throws JsonProcessingException {
        library.lendMediaTo(joeBloggs, twentyThousandLeagues);
        library.lendMediaTo(joeBloggs, blackWidow);

        String json = objectMapper.writeValueAsString(library);

        // Check basic library stats and users
        Library newLibrary = objectMapper.readValue(json, Library.class);
        assertEquals("SE325 Library", newLibrary.getName());
        assertEquals(1, newLibrary.getUsers().size());
        assertEquals(joeBloggs, newLibrary.getUsers().get(0));
        assertNotSame(joeBloggs, newLibrary.getUsers().get(0));
        User newJoe = newLibrary.getUsers().get(0);

        // Check deserialized user's borrowed media count
        assertEquals(2, newJoe.getBorrowedMedia().size());
        assertEquals("Twenty Thousand Leagues Under the Sea", newJoe.getBorrowedMedia().get(0).getTitle());
        assertEquals("Black Widow", newJoe.getBorrowedMedia().get(1).getTitle());
        assertInstanceOf(Book.class, newJoe.getBorrowedMedia().get(0));
        assertInstanceOf(Film.class, newJoe.getBorrowedMedia().get(1));

        // Check borrowed media are equal, but different, objects than the originals
        Book newTwentyThousandLeagues = (Book) newJoe.getBorrowedMedia().get(0);
        Film newBlackWidow = (Film) newJoe.getBorrowedMedia().get(1);
        assertEquals(twentyThousandLeagues, newTwentyThousandLeagues);
        assertEquals(blackWidow, newBlackWidow);
        assertNotSame(twentyThousandLeagues, newTwentyThousandLeagues);
        assertNotSame(blackWidow, newBlackWidow);

        // Check inventory levels
        assertEquals(2, newLibrary.getInventoryCount(newTwentyThousandLeagues));
        assertEquals(0, newLibrary.getInventoryCount(newBlackWidow));

        // Check that image was deserialized correctly
        BufferedImage newBlackWidowImage = newBlackWidow.getCoverImage();
        assertEquals(250, newBlackWidowImage.getWidth());
        assertEquals(371, newBlackWidowImage.getHeight());
    }

    /**
     * Tests the singleton-ness of deserialized Media instances. For example, if a user has borrowed a book, then the
     * entry in that user's borrowedMedia list should be <strong>the same object</strong> as the entry in the library's
     * inventory. This is true before serialization, and we want it to remain true after deserialization.
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testOnlySingleObjectOfEachMedia() throws JsonProcessingException {
        library.lendMediaTo(joeBloggs, twentyThousandLeagues);
        library.lendMediaTo(joeBloggs, blackWidow);

        String json = objectMapper.writeValueAsString(library);

        Library newLibrary = objectMapper.readValue(json, Library.class);
        User newJoe = newLibrary.getUsers().get(0);
        Book newTwentyThousandLeagues = (Book) newJoe.getBorrowedMedia().get(0);
        Film newBlackWidow = (Film) newJoe.getBorrowedMedia().get(1);

        List<InventoryItem> inventory = newLibrary.getInventory();
        assertEquals(2, inventory.size());
        assertSame(newTwentyThousandLeagues, inventory.get(0).getMedia());
        assertSame(newBlackWidow, inventory.get(1).getMedia());
    }

}
