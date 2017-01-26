package be.cleitech.receipt.shoeboxed;

/**
 * @author Pierrick Puimean-Chieze on 08/10/2016.
 */
public class MultipleMainCategoriesException extends Exception {
    public MultipleMainCategoriesException(String message) {
        super(message);
    }

    public MultipleMainCategoriesException(String message, Throwable cause) {
        super(message, cause);
    }
}
