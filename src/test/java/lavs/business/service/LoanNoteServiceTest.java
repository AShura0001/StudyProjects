package lavs.business.service;

import lavs.business.model.LoanNote;
import lavs.business.repository.LoanNoteGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.SessionExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The rules for the notes on a loan, with the mainframe played by a mock. */
class LoanNoteServiceTest {

    private LoanNoteGateway gateway;
    private LoanNoteService service;

    @BeforeEach
    void setUp() {
        gateway = mock(LoanNoteGateway.class);
        service = new LoanNoteService(gateway);
    }

    private static LoanNote summary(long id) {
        return new LoanNote(id, "LN-1", "General", "Group_2", "2026-10-22", null);
    }

    private static LoanNote full(long id) {
        return new LoanNote(id, "LN-1", "General", "Group_2", "2026-10-22", "text " + id);
    }

    private static Map<String, String> problemsOf(ValidationException e) {
        return e.getErrors().stream().collect(Collectors.toMap(ValidationError::getField, ValidationError::getMessage));
    }

    @Test
    void newService_withoutAGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanNoteService(null));
    }

    @Test
    void notesOf_readsEachNoteInFull() {
        when(gateway.listNotes(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(summary(5)), 1, 1));
        when(gateway.findNote("LN-1", 5)).thenReturn(Optional.of(full(5)));

        assertEquals(List.of(full(5)), service.notesOf("LN-1"));
    }

    @Test
    void notesOf_keepsTheSummaryWhenTheFullNoteCannotBeRead() {
        when(gateway.listNotes(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(summary(5)), 1, 1));
        when(gateway.findNote("LN-1", 5)).thenReturn(Optional.empty());

        assertEquals(List.of(summary(5)), service.notesOf("LN-1"));
    }

    @Test
    void notesOf_readsEveryPage() {
        when(gateway.listNotes(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(summary(1)), 2, 1));
        when(gateway.listNotes(eq("LN-1"), eq(2L), anyLong())).thenReturn(new PageResult<>(List.of(summary(2)), 2, 2));
        when(gateway.findNote(eq("LN-1"), anyLong())).thenAnswer(call -> Optional.of(full(call.getArgument(1))));

        assertEquals(List.of(full(1), full(2)), service.notesOf("LN-1"));
    }

    @Test
    void notesOf_aLoanWithNoNotes_isEmpty() {
        when(gateway.listNotes(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(), 0, 1));

        assertTrue(service.notesOf("LN-1").isEmpty());
    }

    @Test
    void find_returnsTheNote() {
        when(gateway.findNote("LN-1", 5)).thenReturn(Optional.of(full(5)));

        assertEquals(Optional.of(full(5)), service.find("LN-1", 5));
    }

    @Test
    void add_tidiesTheTextAndSendsIt() {
        when(gateway.addNote("LN-1", "Visit", "Met the customer.")).thenReturn(full(9));

        assertEquals(full(9), service.add("LN-1", "  Visit ", "  Met the customer. \n"));
    }

    @Test
    void add_withoutATitle_callsTheNoteGeneral() {
        service.add("LN-1", "   ", "Met the customer.");
        service.add("LN-1", null, "Met the customer.");

        verify(gateway, org.mockito.Mockito.times(2)).addNote("LN-1", LoanNoteService.DEFAULT_TITLE, "Met the customer.");
    }

    @Test
    void add_withNoText_isRefusedAndSendsNothing() {
        ValidationException refused = assertThrows(ValidationException.class, () -> service.add("LN-1", "Visit", "  "));

        assertTrue(problemsOf(refused).containsKey(LoanNoteService.LINES_FIELD));
        verify(gateway, never()).addNote(anyString(), anyString(), anyString());
    }

    @Test
    void add_withNullText_isRefused() {
        assertThrows(ValidationException.class, () -> service.add("LN-1", "Visit", null));
    }

    @Test
    void add_atTheLongestAllowed_goesThrough_andOneMoreIsRefused() {
        String longest = "x".repeat(LoanNoteService.MAX_LENGTH);
        service.add("LN-1", "Visit", longest);
        verify(gateway).addNote("LN-1", "Visit", longest);

        ValidationException refused = assertThrows(ValidationException.class,
                () -> service.add("LN-1", "Visit", longest + "x"));
        assertTrue(problemsOf(refused).get(LoanNoteService.LINES_FIELD).contains(String.valueOf(LoanNoteService.MAX_LENGTH)));
    }

    @Test
    void add_withATitleTooLong_isRefused_andEveryProblemIsReportedAtOnce() {
        ValidationException refused = assertThrows(ValidationException.class,
                () -> service.add("LN-1", "t".repeat(LoanNoteService.MAX_TITLE_LENGTH + 1), ""));

        Map<String, String> problems = problemsOf(refused);
        assertTrue(problems.containsKey(LoanNoteService.TITLE_FIELD));
        assertTrue(problems.containsKey(LoanNoteService.LINES_FIELD));
    }

    @Test
    void add_whenTheMainframeRefuses_passesItsReasonOn() {
        when(gateway.addNote(anyString(), anyString(), anyString()))
                .thenThrow(new RecordRejectedException("No such loan.", Map.of()));

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.add("LN-404", "Visit", "Text"));
        assertEquals("No such loan.", refused.getMessage());
    }

    @Test
    void update_sendsTheTidiedChange() {
        when(gateway.updateNote("LN-1", 5, "Visit", "New text")).thenReturn(full(5));

        assertEquals(full(5), service.update("LN-1", 5, "Visit ", " New text"));
    }

    @Test
    void update_withNoText_isRefusedAndSendsNothing() {
        assertThrows(ValidationException.class, () -> service.update("LN-1", 5, "Visit", ""));

        verify(gateway, never()).updateNote(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void delete_asksTheMainframeToRemoveTheNote() {
        service.delete("LN-1", 5);

        verify(gateway).deleteNote("LN-1", 5);
    }

    @Test
    void anUnreachableMainframe_givesAMessageStaffCanActOn() {
        when(gateway.listNotes(anyString(), anyLong(), anyLong())).thenThrow(new MainframeUnavailableException("socket closed"));

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.notesOf("LN-1"));
        assertEquals(LoanNoteService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void anEndedSession_keepsItsOwnMessage() {
        SessionExpiredException ended = new SessionExpiredException();
        doThrow(ended).when(gateway).deleteNote("LN-1", 5);

        assertSame(ended, assertThrows(SessionExpiredException.class, () -> service.delete("LN-1", 5)));
    }

    @Test
    void aBlankLoanNumber_isRejectedBeforeAskingTheMainframe() {
        assertThrows(IllegalArgumentException.class, () -> service.notesOf(" "));
        assertThrows(NullPointerException.class, () -> service.find(null, 1));
        assertThrows(IllegalArgumentException.class, () -> service.add("", "t", "x"));
        assertThrows(IllegalArgumentException.class, () -> service.update(" ", 1, "t", "x"));
        assertThrows(IllegalArgumentException.class, () -> service.delete("", 1));
    }
}
