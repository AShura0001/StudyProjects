package uoa.se325.assignment01.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.Cookie;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Contains methods for generating hashes, UUID strings, JWTs and more.
 */
public class SecurityUtils {

    public static final String AUTH_COOKIE_NAME = "authToken";

    private static final String JWT_SECRET = "SE325A12025S2";

    /**
     * Generates a SHA3-256 hash of the given password string, and returns it as a hex string.
     *
     * @param password the password to hash
     * @return the hex-string representation of the SHA3-256 hash of the provided value
     */
    public static String getSHA256Hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] pwBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = digest.digest(pwBytes);
            return getHexString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA3-256 algorithm not found!", e);
        }
    }

    /**
     * Returns a hex string of the given byte array.
     *
     * @param bytes the array to return as a string
     * @return a hex string
     */
    private static String getHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Generates a random UUID, as a String. Can be used as the "uuid" claim in the JWT,
     * and also saved in the database so we can ensure an incoming user JWT matches a particular
     * user in the database.
     *
     * @return a random UUID string.
     */
    public static String generateRandomUUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a new JWT (JSON Web Token) with the provided values for the "username" and "uuid" claims.
     *
     * @param username the username claim value
     * @param uuid     the uuid claim value
     * @return a valid JWT which can be, for example, added to an auth cookie.
     */
    public static String createJWT(String username, String uuid) {
        Algorithm algo = Algorithm.HMAC256(JWT_SECRET);
        return JWT.create()
                .withClaim("username", username)
                .withClaim("uuid", uuid)
                .sign(algo);
    }

    /**
     * Decodes the given encoded JWT.
     *
     * @param token the encoded JWT to decode
     * @return the decoded JWT
     * @throws JWTVerificationException if encoded JWT is invalid, has been tampered with, or could
     *                                  otherwise not be verified.
     */
    public static DecodedJWT decodeJWT(String token) throws JWTVerificationException {
        Algorithm algo = Algorithm.HMAC256(JWT_SECRET);
        JWTVerifier verifier = JWT.require(algo).build();
        return verifier.verify(token);
    }

    /**
     * Creates an HTTP cookie named {@link #AUTH_COOKIE_NAME}, whose value
     * is a signed JWT with the given username and uuid as claims.
     *
     * @param username the username to add to the JWT
     * @param uuid     the uuid to add to the JWT
     * @return the cookie
     */
    public static Cookie createAuthCookie(String username, String uuid) {
        String jwt = createJWT(username, uuid);
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, jwt);
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        cookie.setMaxAge(604800 * 2);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
        return cookie;
    }

    /**
     * Creates an HTTP cookie named {@link #AUTH_COOKIE_NAME}, with a
     * maxAge of 0 and no data. This can be sent back to the client
     * to log any user out.
     *
     * @return the cookie
     */
    public static Cookie createEmptyAuthCookie() {
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    /**
     * Finds a cookie named {@link #AUTH_COOKIE_NAME} amongst the given cookies, and decodes a
     * {@link DecodedJWT} from it, which will contain the username and uuid of the user trying
     * to authenticate.
     *
     * @param cookies the cookies to examine
     * @return An {@link Optional} with the decoded JWT, if it exists, or empty if the
     * required cookie doesn't exist or the decode fails.
     */
    public static Optional<DecodedJWT> decodeJWTFromCookies(Cookie... cookies) {
        if (cookies == null) return Optional.empty();

        String authToken = Arrays.stream(cookies)
                .filter(c -> AUTH_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (authToken == null) return Optional.empty();

        // Parse JWT from SecurityUtils
        try {
            return Optional.of(JWT.decode(authToken));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
