package info.kgeorgiy.ja.davydov.bank;

/**
 * Thrown when an account balance change would leave the balance outside the
 * permitted range — either negative or overflowing {@code int}.
 */
public class OutOfRangeException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String accountId;
    private final int balance;
    private final int delta;

    public OutOfRangeException(final String accountId, final int balance, final int delta) {
        super("Balance change out of range on " + accountId
                + "\n: balance=" + balance + ", delta=" + delta);
        this.accountId = accountId;
        this.balance = balance;
        this.delta = delta;
    }

    public String getAccountId() { return accountId; }
    public int getBalance()      { return balance; }
    public int getDelta()        { return delta; }
}
