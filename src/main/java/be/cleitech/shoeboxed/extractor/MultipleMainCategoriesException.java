package be.cleitech.shoeboxed.extractor;

/**
 * Created by Pierrick on 08/10/2016.
 */
public class MultipleMainCategoriesException extends Exception {
    public MultipleMainCategoriesException(String message) {
        super(message);
    }

    public MultipleMainCategoriesException(String message, Throwable cause) {
        super(message, cause);
    }
}
