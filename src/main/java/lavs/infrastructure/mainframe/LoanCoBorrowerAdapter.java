package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.Connection;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.DeleteCoborrower;
import mainframe.client.messages.loanCoborrower.List;
import mainframe.client.messages.loanCoborrower.Retrieve;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Talks to the mainframe for {@link LoanCoBorrowerGateway}.
 */
public class LoanCoBorrowerAdapter implements LoanCoBorrowerGateway {

    private final Connection connection;

    public LoanCoBorrowerAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<CoBorrower> findCoBorrower(String loanNumber, long id) {
        Retrieve retrieve = new Retrieve(id, loanNumber);
        if (!MainframeCalls.send(retrieve, connection).getWasSuccessful()) {
            return Optional.empty();
        }
        return Optional.of(new CoBorrower(
                retrieve.getId(),
                loanNumberOr(retrieve.getLoanNumber(), loanNumber),
                retrieve.getCoborrowerNumber(),
                retrieve.getCoborrowerName(),
                toType(retrieve.getType())));
    }

    @Override
    public PageResult<CoBorrower> listCoBorrowers(String loanNumber, long page, long pageSize) {
        List list = new List(loanNumber, page, pageSize);
        if (!MainframeCalls.send(list, connection).getWasSuccessful()) {
            return PageResult.empty(page);
        }
        java.util.List<CoBorrower> coBorrowers = new ArrayList<>();
        for (List.Item item : list.getItems()) {
            coBorrowers.add(new CoBorrower(
                    item.getId(),
                    loanNumberOr(list.getLoanNumber(), loanNumber),
                    item.getCoborrowerNumber(),
                    item.getCoborrowerName(),
                    toType(item.getType())));
        }
        return new PageResult<>(coBorrowers, list.getTotal(), list.getPage());
    }

    @Override
    public CoBorrower addCoBorrower(String loanNumber, String coBorrowerNumber, CoBorrowerType type) {
        AddCoborrower add = new AddCoborrower(coBorrowerNumber, loanNumber, toMainframeType(type));
        MainframeCalls.sendChange(add, connection);
        return findCoBorrower(loanNumber, add.getId())
                .orElseGet(() -> new CoBorrower(add.getId(), loanNumber, coBorrowerNumber, null, type));
    }

    @Override
    public void deleteCoBorrower(String loanNumber, long id) {
        MainframeCalls.sendChange(new DeleteCoborrower(id, loanNumber), connection);
    }

    static String toMainframeType(CoBorrowerType type) {
        return switch (type) {
            case COBORROWER -> AddCoborrower.TYPE_COBORROWER;
            case GUARANTOR -> AddCoborrower.TYPE_GUARANTOR;
        };
    }

    static CoBorrowerType toType(String mainframeType) {
        if (AddCoborrower.TYPE_COBORROWER.equalsIgnoreCase(mainframeType)) {
            return CoBorrowerType.COBORROWER;
        }
        if (AddCoborrower.TYPE_GUARANTOR.equalsIgnoreCase(mainframeType)) {
            return CoBorrowerType.GUARANTOR;
        }
        throw new RecordRejectedException(
                "The mainframe sent a co-borrower type that is not known: " + mainframeType,
                Map.of());
    }

    private static String loanNumberOr(String sent, String asked) {
        return sent == null ? asked : sent;
    }
}
