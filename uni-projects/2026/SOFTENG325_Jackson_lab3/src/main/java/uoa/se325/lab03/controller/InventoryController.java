package uoa.se325.lab03.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab03.model.library.InventoryItem;
import uoa.se325.lab03.model.library.Media;
import uoa.se325.lab03.service.LibraryDB;

import java.awt.image.BufferedImage;
import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final LibraryDB libraryDB;

    @Autowired
    public InventoryController(LibraryDB libraryDB) {
        this.libraryDB = libraryDB;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems(@RequestParam(required = false) String search,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(defaultValue = "5") int size) {
        return getInventoryResponse(search, false, page, size);
    }

    @GetMapping(value = "", produces = "text/csv")
    public ResponseEntity<String> getAllInventoryItemsCsv(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(defaultValue = "5") int size) {
        return getInventoryCsvResponse(search, false, page, size);
    }

    @GetMapping(value = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAvailableInventoryItems(@RequestParam(required = false) String search,
                                                                         @RequestParam(required = false) Integer page,
                                                                         @RequestParam(defaultValue = "5") int size) {
        return getInventoryResponse(search, true, page, size);
    }

    @GetMapping(value = "/available", produces = "text/csv")
    public ResponseEntity<String> getAvailableInventoryItemsCsv(@RequestParam(required = false) String search,
                                                              @RequestParam(required = false) Integer page,
                                                              @RequestParam(defaultValue = "5") int size) {
        return getInventoryCsvResponse(search, true, page, size);
    }

    private ResponseEntity<List<InventoryItem>> getInventoryResponse(String search, boolean onlyAvailable, Integer page, int size) {
        List<InventoryItem> filteredItems = filterInventoryItems(search, onlyAvailable);
        return paginateInventoryItems(filteredItems, page, size);
    }

    private ResponseEntity<String> getInventoryCsvResponse(String search, boolean onlyAvailable, Integer page, int size) {
        List<InventoryItem> filteredItems = filterInventoryItems(search, onlyAvailable);
        ResponseEntity<List<InventoryItem>> entity = paginateInventoryItems(filteredItems, page, size);
        String csv = convertInventoryToCsv(entity.getBody() == null ? List.of() : entity.getBody());
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(entity.getHeaders());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv);
    }

    private ResponseEntity<List<InventoryItem>> paginateInventoryItems(List<InventoryItem> filteredItems, Integer page, int size) {
        if (page == null) {
            return ResponseEntity.ok(filteredItems);
        }

        if (page <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number must be greater than 0.");
        }

        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be greater than 0.");
        }

        int fromIndex = (page - 1) * size;
        if (fromIndex >= filteredItems.size()) {
            return ResponseEntity.ok().body(List.of());
        }

        int toIndex = Math.min(fromIndex + size, filteredItems.size());
        List<InventoryItem> pageItems = filteredItems.subList(fromIndex, toIndex);

        HttpHeaders headers = new HttpHeaders();
        if (page > 1) {
            headers.add("Prev-Page", String.valueOf(page - 1));
        }
        if (toIndex < filteredItems.size()) {
            headers.add("Next-Page", String.valueOf(page + 1));
        }

        return ResponseEntity.ok().headers(headers).body(pageItems);
    }

    private String convertInventoryToCsv(List<InventoryItem> items) {
        StringBuilder csv = new StringBuilder();
        csv.append("BarCode,Type,Title,Count\n");
        for (InventoryItem item : items) {
            Media media = item.getMedia();
            String mediaType = media instanceof uoa.se325.lab03.model.library.Book ? "Book" : "Film";
            csv.append(media.getBarCode()).append(',')
                    .append(mediaType).append(',')
                    .append(media.getTitle()).append(',')
                    .append(item.getCount()).append('\n');
        }
        return csv.toString();
    }

    private List<InventoryItem> filterInventoryItems(String search, boolean onlyAvailable) {
        return libraryDB.getLibrary().getInventory()
                .stream()
                .filter(item -> (search == null || item.getMedia().getTitle().toLowerCase().contains(search.toLowerCase())) &&
                        (!onlyAvailable || item.getCount() > 0))
                .toList();
    }

    @GetMapping(value = "/{barCode}/cover-image", produces = MediaType.IMAGE_PNG_VALUE)
    public BufferedImage getInventoryCoverImage(@PathVariable String barCode) {
        InventoryItem item = libraryDB.getLibrary().getInventory().stream()
                .filter(inv -> inv.getMedia().getBarCode().equals(barCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found."));

        if (item.getMedia().getCoverImage() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item has no cover image.");
        }

        return item.getMedia().getCoverImage();
    }

    @GetMapping("/{barCode}")
    public InventoryItem getInventoryItemByBarCode(@PathVariable String barCode) {
        return libraryDB.getLibrary().getInventory().stream()
                .filter(item -> item.getMedia().getBarCode().equals(barCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item with barCode " + barCode + " not found."));
    }
}
