package uoa.se325.assignment01.dto;

// Request body shape shared by login and account creation.
public record UserCredentials(String username, String password) {
}
