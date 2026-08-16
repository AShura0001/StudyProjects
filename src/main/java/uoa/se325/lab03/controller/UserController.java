package uoa.se325.lab03.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab03.dto.BorrowRequest;
import uoa.se325.lab03.model.library.LibraryException;
import uoa.se325.lab03.model.library.Media;
import uoa.se325.lab03.model.library.User;
import uoa.se325.lab03.service.LibraryDB;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final LibraryDB libraryDB;
    private final InventoryController inventoryController;

    @Autowired
    public UserController(LibraryDB libraryDB, InventoryController inventoryController) {
        this.libraryDB = libraryDB;
        this.inventoryController = inventoryController;
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return libraryDB.getLibrary().getUsers();
    }

    @GetMapping("/{cardNum}")
    public User getUserByLibraryCardNumber(@PathVariable("cardNum") String cardNum) {
        return libraryDB.getLibrary().getUsers().stream()
                .filter(user -> user.getLibraryCardNumber().equals(cardNum))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping("/{cardNum}/borrowed-media")
    public List<Media> getBorrowedMediaByUser(@PathVariable("cardNum") String cardNum) {
        User user = getUserByLibraryCardNumber(cardNum);
        return user.getBorrowedMedia();
    }

    @PatchMapping("/{cardNum}/borrowed-media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrowMedia(@PathVariable("cardNum") String cardNum, @RequestBody BorrowRequest request) {
        try {
            User user = getUserByLibraryCardNumber(cardNum);
            Media media = inventoryController.getInventoryItemByBarCode(request.getBarCode()).getMedia();
            libraryDB.getLibrary().lendMediaTo(user, media);
        } catch (LibraryException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{cardNum}/borrowed-media/{barCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnMedia(@PathVariable("cardNum") String cardNum, @PathVariable("barCode") String barCode) {
        try {
            User user = getUserByLibraryCardNumber(cardNum);
            Media media = inventoryController.getInventoryItemByBarCode(barCode).getMedia();
            libraryDB.getLibrary().returnMediaFrom(user, media);
        } catch (LibraryException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
}
