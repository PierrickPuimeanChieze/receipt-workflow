package be.cleitech.receipt;

import javax.mail.MessagingException;

/**
 * Created by pierrick on 07.02.17.
 */

public class RuntimeMeassingException extends RuntimeException{
    public RuntimeMeassingException(MessagingException e) {
        super(e);
    }
}
