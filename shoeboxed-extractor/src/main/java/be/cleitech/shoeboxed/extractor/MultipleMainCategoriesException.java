package be.cleitech.shoeboxed.extractor;

/**
 * @author Pierrick Puimean-Chieze on 08/10/2016.
 */
class MultipleMainCategoriesException extends Exception {
    MultipleMainCategoriesException(String message) {
        super(message);
    }

    MultipleMainCategoriesException(String message, Throwable cause) {
        super(message, cause);
    }
}
