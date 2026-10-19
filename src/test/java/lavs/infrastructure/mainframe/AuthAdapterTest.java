package lavs.infrastructure.mainframe;

import lavs.business.repository.InvalidCredentialsException;
import lavs.business.repository.MainframeUnavailableException;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAdapterTest {

    @Mock
    private Connection connection;

    private AuthAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthAdapter(connection);
    }

    @Test
    void constructor_withNullConnection_throws() {
        assertThrows(NullPointerException.class, () -> new AuthAdapter(null));
    }

    @Test
    void login_whenMainframeAccepts_returnsUsername() throws IOException {
        when(connection.Login("Group_2", "bug")).thenReturn(new Status(1));

        assertEquals("Group_2", adapter.login("Group_2", "bug"));
    }

    @Test
    void login_whenMainframeRefuses_throwsInvalidCredentialsWithReason() throws IOException {
        when(connection.Login("Group_2", "wrong")).thenReturn(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), "Invalid password", 1));

        InvalidCredentialsException e = assertThrows(InvalidCredentialsException.class,
                () -> adapter.login("Group_2", "wrong"));
        assertEquals("Invalid password", e.getMessage());
    }

    @Test
    void login_whenRefusedWithoutReason_usesDefaultMessage() throws IOException {
        when(connection.Login("Group_2", "wrong")).thenReturn(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), "", 1));

        InvalidCredentialsException e = assertThrows(InvalidCredentialsException.class,
                () -> adapter.login("Group_2", "wrong"));
        assertEquals(AuthAdapter.REFUSED_MESSAGE, e.getMessage());
    }

    @Test
    void login_whenNetworkFails_throwsUnavailable() throws IOException {
        when(connection.Login("Group_2", "bug")).thenReturn(
                new Status(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), "down", 1));

        assertThrows(MainframeUnavailableException.class, () -> adapter.login("Group_2", "bug"));
    }

    @Test
    void login_whenReplyCannotBeParsed_throwsUnavailable() throws IOException {
        when(connection.Login("Group_2", "bug")).thenReturn(
                new Status(StatusMessage.UNABLE_TO_PARSE.getCode(), "bad reply", 1));

        assertThrows(MainframeUnavailableException.class, () -> adapter.login("Group_2", "bug"));
    }

    @Test
    void login_whenConnectionThrowsIOException_throwsUnavailable() throws IOException {
        IOException cause = new IOException("boom");
        when(connection.Login("Group_2", "bug")).thenThrow(cause);

        MainframeUnavailableException e = assertThrows(MainframeUnavailableException.class,
                () -> adapter.login("Group_2", "bug"));
        assertEquals(cause, e.getCause());
    }

    @Test
    void login_whenConnectionThrowsRuntimeException_throwsUnavailable() throws IOException {
        when(connection.Login("Group_2", "bug")).thenThrow(new NumberFormatException("lastNumber"));

        assertThrows(MainframeUnavailableException.class, () -> adapter.login("Group_2", "bug"));
    }

    @Test
    void logout_clearsTheConnectionSession() {
        adapter.logout();

        verify(connection).Logout();
    }
}
