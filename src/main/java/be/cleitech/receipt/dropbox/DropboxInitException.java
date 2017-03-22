package be.cleitech.receipt.dropbox;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by pierrick on 07.02.17.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DropboxInitException extends RuntimeException {
    public DropboxInitException(String s) {
        super(s);
    }
}
