package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * How far a loan has been repaid: its schedule with each repayment's status, and the
 * totals worked out from the repayments marked paid.
 *
 * @param repayments the schedule in payment order, each with its status
 * @param paymentsMade how many repayments are marked paid
 * @param principalPaid the principal part of the paid repayments
 * @param interestPaid the interest part of the paid repayments
 * @param outstanding the amount borrowed less the principal paid, never below zero
 * @param nextDue the day the next unpaid repayment is due, or null when all are paid
 */
public record RepaymentProgress(
        List<Repayment> repayments,
        int paymentsMade,
        BigDecimal principalPaid,
        BigDecimal interestPaid,
        BigDecimal outstanding,
        LocalDate nextDue) {

    /** One repayment of the schedule and where it stands. */
    public record Repayment(RepaymentEntry entry, RepaymentStatus status) {
        public Repayment {
            Objects.requireNonNull(entry, "entry must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }

    public RepaymentProgress {
        repayments = List.copyOf(Objects.requireNonNull(repayments, "repayments must not be null"));
        Objects.requireNonNull(principalPaid, "principalPaid must not be null");
        Objects.requireNonNull(interestPaid, "interestPaid must not be null");
        Objects.requireNonNull(outstanding, "outstanding must not be null");
    }

    /** @return how many repayments the schedule has */
    public int paymentsTotal() {
        return repayments.size();
    }

    /** @return principal and interest paid together */
    public BigDecimal totalPaid() {
        return principalPaid.add(interestPaid);
    }

    /** @return the day the next unpaid repayment is due, or empty when all are paid */
    public Optional<LocalDate> nextDueDate() {
        return Optional.ofNullable(nextDue);
    }
}
