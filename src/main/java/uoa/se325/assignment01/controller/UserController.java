package uoa.se325.assignment01.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uoa.se325.assignment01.auth.SecurityUtils;
import uoa.se325.assignment01.dto.UserCredentials;
import uoa.se325.assignment01.model.User;
import uoa.se325.assignment01.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody UserCredentials credentials, HttpServletResponse response) {
        String passHash = SecurityUtils.getSHA256Hash(credentials.password());

        User user = userRepository.findByUsername(credentials.username())
                .filter(u -> u.getPassHash().equals(passHash))
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Cookie authCookie = SecurityUtils.createAuthCookie(user.getUsername(), SecurityUtils.generateRandomUUIDString());
        response.addCookie(authCookie);

        return ResponseEntity.noContent().build();
    }
}
