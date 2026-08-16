package uoa.se325.lab03.model.library;

import java.util.Objects;

public class InventoryItem {

    private Media media;

    private int count;

    public InventoryItem(Media media, int count) {
        this.media = media;
        this.count = count;
    }

    public Media getMedia() {
        return media;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return count == that.count && media.equals(that.media);
    }

    @Override
    public int hashCode() {
        return Objects.hash(media, count);
    }
}
