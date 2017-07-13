package be.cleitech.receipt;

import be.cleitech.receipt.tasks.ProcessTaskResult;

import javax.mail.MessagingException;
import java.util.Collection;

/**
 * Created by ppc on 1/26/2017.
 */
public interface MailManager {
    void sentExtractionResults(Collection<String> fileList) throws MessagingException;

    void sentPublishOcrProcess(Collection<ProcessTaskResult> fileList) throws MessagingException;
    void sendErrorMessage(String[] operationArgs, String errorContent) throws MessagingException;
}
