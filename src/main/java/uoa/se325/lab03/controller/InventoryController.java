package uoa.se325.lab03.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab03.model.library.InventoryItem;
import uoa.se325.lab03.service.LibraryDB;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final LibraryDB libraryDB;

    @Autowired
    public InventoryController(LibraryDB libraryDB) {
        this.libraryDB = libraryDB;
    }

    @GetMapping
    public List<InventoryItem> getAllInventoryItems(@RequestParam(required = false) String search) {
        return filterInventoryItems(search, false);
    }

    @GetMapping("/available")
    public List<InventoryItem> getAvailableInventoryItems(@RequestParam(required = false) String search) {
        return filterInventoryItems(search, true);
    }

    private List<InventoryItem> filterInventoryItems(String search, boolean onlyAvailable) {
        return libraryDB.getLibrary().getInventory()
                .stream()
                .filter(item -> (search == null || item.getMedia().getTitle().toLowerCase().contains(search.toLowerCase())) &&
                        (!onlyAvailable || item.getCount() > 0))
                .toList();
    }

    @GetMapping("/{barCode}")
    public InventoryItem getInventoryItemByBarCode(@PathVariable String barCode) {
        return libraryDB.getLibrary().getInventory().stream()
                .filter(item -> item.getMedia().getBarCode().equals(barCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item with barCode " + barCode + " not found."));
    }
}
