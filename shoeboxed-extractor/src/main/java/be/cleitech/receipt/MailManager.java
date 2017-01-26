package be.cleitech.receipt;

import javax.mail.MessagingException;
import java.util.Collection;

/**
 * Created by ppc on 1/26/2017.
 */
public interface MailManager {
    void sentExtractionResults(Collection<String> fileList) throws MessagingException;
}
