package lavs.infrastructure.mainframe;

import lavs.business.model.LoanNote;
import lavs.business.repository.LoanNoteGateway;
import lavs.business.repository.PageResult;
import mainframe.client.Connection;
import mainframe.client.messages.loan.AddNote;
import mainframe.client.messages.loan.DeleteNote;
import mainframe.client.messages.loan.UpdateNote;
import mainframe.client.messages.loanNote.List;
import mainframe.client.messages.loanNote.Retrieve;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Talks to the mainframe for {@link LoanNoteGateway}.
 */
public class LoanNoteAdapter implements LoanNoteGateway {

    private final Connection connection;

    public LoanNoteAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<LoanNote> findNote(String loanNumber, long id) {
        Retrieve retrieve = new Retrieve(id, loanNumber);
        if (!MainframeCalls.send(retrieve, connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return Optional.of(new LoanNote(
                retrieve.getId(),
                retrieve.getLoanNumber() == null ? loanNumber : retrieve.getLoanNumber(),
                retrieve.getType(),
                retrieve.getAuthor(),
                retrieve.getDateAdded(),
                retrieve.getLines()));
    }

    @Override
    public PageResult<LoanNote> listNotes(String loanNumber, long page, long pageSize) {
        List list = new List(loanNumber, page, pageSize);
        if (!MainframeCalls.send(list, connection).getWasSuccessful()) {
            return PageResult.empty(page);
        }
        String listLoanNumber = list.getLoanNumber() == null ? loanNumber : list.getLoanNumber();
        java.util.List<LoanNote> notes = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            notes.add(new LoanNote(
                    item.getId(), listLoanNumber, item.getType(), item.getAuthor(), item.getDateAdded(), null));
        }
        return new PageResult<>(notes, list.getTotal(), list.getPage());
    }

    @Override
    public LoanNote addNote(String loanNumber, String type, String lines) {
        AddNote add = new AddNote(lines, loanNumber, type);
        MainframeCalls.sendChange(add, connection);
        return findNote(loanNumber, add.getId())
                .orElseGet(() -> new LoanNote(add.getId(), loanNumber, type, add.getAuthor(), null, lines));
    }

    @Override
    public LoanNote updateNote(String loanNumber, long id, String type, String lines) {
        UpdateNote update = new UpdateNote(id, lines, loanNumber, type);
        MainframeCalls.sendChange(update, connection);
        return findNote(loanNumber, id)
                .orElseGet(() -> new LoanNote(id, loanNumber, type, update.getAuthor(), null, lines));
    }

    @Override
    public void deleteNote(String loanNumber, long id) {
        MainframeCalls.sendChange(new DeleteNote(id, loanNumber), connection);
    }
}
