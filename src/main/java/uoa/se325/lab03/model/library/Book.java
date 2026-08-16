package uoa.se325.lab03.model.library;

import java.util.Objects;

public class Book extends Media {

    private int numPages;
    private String author;
    private String publisher;

    public Book() {

    }

    public Book(String barCode, String title, int numPages, String author, String publisher, Genre... genres) {
        super(barCode, title, genres);
        this.numPages = numPages;
        this.author = author;
        this.publisher = publisher;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Book book = (Book) o;
        return numPages == book.numPages && author.equals(book.author) && publisher.equals(book.publisher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), numPages, author, publisher);
    }
}
