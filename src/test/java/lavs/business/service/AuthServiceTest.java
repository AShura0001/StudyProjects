package lavs.business.service;

import lavs.business.model.AuthResult;
import lavs.business.model.Session;
import lavs.business.repository.AuthGateway;
import lavs.business.repository.InvalidCredentialsException;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T09:00:00Z");

    @Mock
    private AuthGateway gateway;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(gateway, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void login_withCorrectDetails_startsSession() {
        when(gateway.login("Group_2", "bug")).thenReturn("Group_2");

        AuthResult result = service.login("Group_2", "bug");

        assertTrue(result.success());
        assertTrue(service.isLoggedIn());
        assertEquals(new Session("Group_2", NOW), service.currentSession().orElseThrow());
    }

    @Test
    void login_withSurroundingSpaces_sendsTrimmedUsername() {
        when(gateway.login("Group_2", "bug")).thenReturn("Group_2");

        assertTrue(service.login("  Group_2 ", "bug").success());
    }

    @Test
    void login_whenMainframeConfirmsNoName_usesEnteredUsername() {
        when(gateway.login("Group_2", "bug")).thenReturn(null);

        service.login("Group_2", "bug");

        assertEquals("Group_2", service.currentSession().orElseThrow().username());
    }

    @Test
    void login_whenMainframeConfirmsBlankName_usesEnteredUsername() {
        when(gateway.login("Group_2", "bug")).thenReturn("  ");

        service.login("Group_2", "bug");

        assertEquals("Group_2", service.currentSession().orElseThrow().username());
    }

    @Test
    void login_withWrongDetails_isRejected() {
        when(gateway.login("Group_2", "wrong"))
                .thenThrow(new InvalidCredentialsException("Unknown or invalid user"));

        AuthResult result = service.login("Group_2", "wrong");

        assertFalse(result.success());
        assertEquals(AuthService.INVALID_CREDENTIALS, result.message());
        assertFalse(service.isLoggedIn());
    }

    @Test
    void login_whenMainframeUnreachable_tellsUserWhy() {
        when(gateway.login("Group_2", "bug"))
                .thenThrow(new MainframeUnavailableException("timed out"));

        AuthResult result = service.login("Group_2", "bug");

        assertFalse(result.success());
        assertEquals(AuthService.UNAVAILABLE, result.message());
        assertFalse(service.isLoggedIn());
    }

    @Test
    void login_withBlankUsername_isRejectedWithoutCallingMainframe() {
        AuthResult result = service.login("   ", "bug");

        assertEquals(AuthService.MISSING_DETAILS, result.message());
        verify(gateway, never()).login(any(), any());
    }

    @Test
    void login_withNullUsername_isRejectedWithoutCallingMainframe() {
        assertEquals(AuthService.MISSING_DETAILS, service.login(null, "bug").message());
        verify(gateway, never()).login(any(), any());
    }

    @Test
    void login_withEmptyPassword_isRejectedWithoutCallingMainframe() {
        assertEquals(AuthService.MISSING_DETAILS, service.login("Group_2", "").message());
        verify(gateway, never()).login(any(), any());
    }

    @Test
    void login_withNullPassword_isRejectedWithoutCallingMainframe() {
        assertEquals(AuthService.MISSING_DETAILS, service.login("Group_2", null).message());
        verify(gateway, never()).login(any(), any());
    }

    @Test
    void login_failingAfterEarlierSuccess_keepsEarlierSession() {
        when(gateway.login("Group_2", "bug")).thenReturn("Group_2");
        when(gateway.login("Group_2", "wrong"))
                .thenThrow(new InvalidCredentialsException("Unknown or invalid user"));
        service.login("Group_2", "bug");

        service.login("Group_2", "wrong");

        assertEquals("Group_2", service.currentSession().orElseThrow().username());
    }

    @Test
    void logout_afterLogin_endsSession() {
        when(gateway.login("Group_2", "bug")).thenReturn("Group_2");
        service.login("Group_2", "bug");

        service.logout();

        assertFalse(service.isLoggedIn());
        assertTrue(service.currentSession().isEmpty());
        verify(gateway).logout();
    }

    @Test
    void logout_whenNotLoggedIn_doesNothing() {
        service.logout();

        assertFalse(service.isLoggedIn());
        verify(gateway, never()).logout();
    }

    @Test
    void newService_withNullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new AuthService(null));
    }

    @Test
    void newService_withDefaultClock_stampsCurrentTime() {
        when(gateway.login("Group_2", "bug")).thenReturn("Group_2");
        AuthService systemClockService = new AuthService(gateway);
        Instant before = Instant.now();

        systemClockService.login("Group_2", "bug");

        assertFalse(systemClockService.currentSession().orElseThrow().loggedInAt().isBefore(before));
    }
}
