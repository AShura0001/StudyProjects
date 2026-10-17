package lavs.infrastructure.mainframe;

import lavs.business.model.Note;
import lavs.business.repository.CustomerNoteGateway;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.customer.DeleteNote;
import mainframe.client.messages.customer.UpdateNote;
import mainframe.client.messages.customerNote.Retrieve;

import java.io.IOException;
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
            return mapMessageNote(retrieve);
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
            return mapMessageNote(retrieve);
        }
        return Optional.empty();
    }

    @Override
    public void deleteNoteByCustomerNumberAndId(long id, String customerNumber) throws IOException {
        DeleteNote deleteNote = new DeleteNote(customerNumber, id);
        Status status = deleteNote.send(connection);
    }

    public Optional<Note> mapMessageNote(Message message) {
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
