package lavs.business.model;

/** Where one repayment of a loan's schedule stands. */
public enum RepaymentStatus {
    /** A payment has been recorded for it. */
    PAID,
    /** The first repayment not yet paid. */
    DUE,
    /** Later than the one due. */
    UPCOMING
}
