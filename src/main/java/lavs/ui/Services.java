package lavs.ui;

import lavs.business.service.AuthService;
import lavs.ui.stub.StubAuthGateway;

/**
 * The one place the screens get their business-layer services from. To go
 * live, swap the stub gateway below for the real one; no screen changes.
 */
public final class Services {

    private static final AuthService AUTH = new AuthService(new StubAuthGateway());

    private Services() {
    }

    public static AuthService auth() {
        return AUTH;
    }

    /** Shows the logged-out screen straight away and ends the session in the background. */
    public static void logOut() {
        Thread worker = new Thread(AUTH::logout, "logout");
        worker.setDaemon(true);
        worker.start();
        Navigator.show("loggedout");
    }
}
