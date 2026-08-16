package uoa.se325.lab03.model.library;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Library {

    @JsonProperty("name")
    private String name;
    @JsonProperty("inventory")
    private List<InventoryItem> inventory = new ArrayList<>();
    @JsonProperty("users")
    private List<User> users = new ArrayList<>();

    public Library() {
    }

    public Library(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public int getInventoryCount(Media media) {
        return inventory.stream()
                .filter(i -> i.getMedia().equals(media))
                .findFirst()
                .map(InventoryItem::getCount)
                .orElse(0);
    }

    public List<InventoryItem> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public void addToInventory(Media media, int count) {
        Optional<InventoryItem> item = inventory.stream().filter(i -> i.getMedia().equals(media)).findFirst();
        if (item.isPresent()) {
            item.get().setCount(item.get().getCount() + count);
        } else {
            inventory.add(new InventoryItem(media, count));
        }
    }

    public void lendMediaTo(User user, Media media) throws LibraryException {
        Optional<InventoryItem> item = inventory.stream().filter(i -> i.getMedia().equals(media)).findFirst();
        if (item.isEmpty() || item.get().getCount() == 0) {
            throw new LibraryException("Given media is not in inventory!");
        }
        user.addMedia(media);
        item.get().setCount(item.get().getCount() - 1);
    }

    public void returnMediaFrom(User user, Media media) throws LibraryException {
        Optional<InventoryItem> item = inventory.stream().filter(i -> i.getMedia().equals(media)).findFirst();
        if (item.isEmpty()) {
            throw new LibraryException("Given media is not in inventory!");
        }
        if (!user.getBorrowedMedia().contains(media)) {
            throw new LibraryException("Given media wasn't borrowed by given user!");
        }
        user.removeMedia(media);
        item.get().setCount(item.get().getCount() + 1);
    }
}
