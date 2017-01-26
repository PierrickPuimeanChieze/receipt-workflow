package be.cleitech.receipt.shoeboxed.domain;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
public class User {

    private Account[] accounts;

    public Account[] getAccounts() {
        return accounts;
    }

    public void setAccounts(Account[] accounts) {
        this.accounts = accounts;
    }
}
