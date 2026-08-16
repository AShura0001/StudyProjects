package uoa.se325.lab03.service;

import uoa.se325.lab03.model.library.Library;

public class LibraryDB {

    private final Library library;

    public LibraryDB() {
        this(new Library("SE325 Library"));
    }

    public LibraryDB(Library library) {
        this.library = library;
    }

    public Library getLibrary() {
        return library;
    }
}
