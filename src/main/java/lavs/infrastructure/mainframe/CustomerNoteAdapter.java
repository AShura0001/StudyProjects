package lavs.infrastructure.mainframe;

import lavs.business.model.Note;
import lavs.business.repository.CustomerNoteGateway;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.AddNote;
import mainframe.client.messages.customer.DeleteNote;
import mainframe.client.messages.customer.UpdateNote;
import mainframe.client.messages.customerNote.List;
import mainframe.client.messages.customerNote.Retrieve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class CustomerNoteAdapter implements CustomerNoteGateway {

    private final Connection connection;

    public CustomerNoteAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Note> findNoteByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        Retrieve retrieve = new Retrieve(id, customerNumber);
        Status status = retrieve.send(connection);
        if (status.getWasSuccessful()) {
            return mapMessageToNote(retrieve);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Note> updateCustomerNote(Note note) throws IOException {
        UpdateNote updateNote = new UpdateNote(
                note.get_customerNumber(),
                note.get_id(),
                note.get_lines(),
                note.get_type()
        );
        Status status = updateNote.send(connection);
        if (status.getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(note.get_id(), note.get_customerNumber());
            retrieve.send(connection);
            return mapMessageToNote(retrieve);
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteNoteByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        DeleteNote deleteNote = new DeleteNote(customerNumber, id);
        return deleteNote.send(connection).getWasSuccessful();
    }

    @Override
    public Optional<Note> addCustomerNote(Note note) throws IOException {
        AddNote addNote = new AddNote(
                note.get_customerNumber(),
                note.get_lines(),
                note.get_type()
        );
        if(addNote.send(connection).getWasSuccessful()) {
            Retrieve retrieve = new Retrieve(addNote.getId(), note.get_customerNumber());
            if (retrieve.send(connection).getWasSuccessful()) {
                return mapMessageToNote(retrieve);
            }
        }
        return Optional.empty();
    }

    @Override
    public CustomerSearchResult<Note> ListCustomerNotes(long page, String customerNumber, long page_size) throws IOException {
        List list = new List(customerNumber, page, page_size);
        if (!list.send(connection).getWasSuccessful()) {
            return new CustomerSearchResult<>(
                    java.util.List.of(),
                    0,
                    page
            );
        }

        return new CustomerSearchResult<>(
                mapItemsToNoteList(list),
                list.getTotal(),
                list.getPage()
        );
    }

    private ArrayList<Note> mapItemsToNoteList(List list) {
        ArrayList<Note> items = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            Note note = new Note();
            note.set_id(item.getId());
            note.set_author(item.getAuthor());
            note.set_dateAdded(item.getDateAdded());
            note.set_numberOfLines(item.getNumberOfLines());
            note.set_type(item.getType());
            note.set_customerNumber(list.getCustomerNumber());
            note.set_customerName(list.getCustomerName());
            items.add(note);
        }
        return items;
    }

    public Optional<Note> mapMessageToNote(Message message) {
        if (message instanceof Retrieve retrieve) {
            Note note = new Note(
                    retrieve.getAuthor(),
                    retrieve.getCustomerName(),
                    retrieve.getCustomerNumber(),
                    retrieve.getDateAdded(),
                    retrieve.getId(),
                    retrieve.getLines(),
                    retrieve.getNumberOfLines(),
                    retrieve.getType()
            );

            return Optional.of(note);

        }
        return Optional.empty();
    }
}
