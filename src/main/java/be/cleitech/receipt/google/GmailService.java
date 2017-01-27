package be.cleitech.receipt.google;

import be.cleitech.receipt.MailManager;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.ui.velocity.VelocityEngineUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Created by ppc on 1/26/2017.
 */

public class GmailService implements MailManager {


    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();
    private final Gmail gmail;
    private JsonFactory jsonFactory;
    private VelocityEngine velocityEngine;
    private final String uploadResultDest;
    private final String uploadResultCc;
    private final String uploadResultSubject;
    private String uploadResultFrom;

    public GmailService(

            HttpTransport httpTransport,
            Credential credential,
            JsonFactory jsonFactory, VelocityEngine velocityEngine,String applicationName,
            String uploadResultDest,
            String uploadResultCc,
            String uploadResultFrom,
            String uploadResultSubject) throws GeneralSecurityException, IOException {
        this.velocityEngine = velocityEngine;
        this.uploadResultDest = uploadResultDest;
        this.uploadResultCc = uploadResultCc;
        this.uploadResultFrom = uploadResultFrom;
        this.uploadResultSubject = uploadResultSubject;

        gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();
    }


    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    private static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to       email address of the receiver
     * @param from     email address of the sender, the mailbox account
     * @param subject  subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    private static MimeMessage createEmail(String to,
                                          String from,
                                          String cc,
                                          String subject,
                                          String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.addRecipient(javax.mail.Message.RecipientType.CC,
                new InternetAddress(cc));
        email.setText(bodyText);
        return email;
    }

    public void sentExtractionResults(Collection<String> fileList) throws MessagingException {

        Map<String, Object> model = new HashMap<>();
        model.put("fileList", fileList);
        String text = VelocityEngineUtils.mergeTemplateIntoString(
                velocityEngine, "velocity/uploadResult.mailTemplate.vm", "UTF-8", model);
        try {
            MimeMessage email = createEmail(
                    uploadResultDest,
                    uploadResultFrom,
                    uploadResultCc,
                    uploadResultSubject, text);
            Message message = createMessageWithEmail(email);
            Message sentMessage = gmail.users().messages().send("me", message).execute();
            System.out.println("Message id: " + sentMessage.getId());
            System.out.println(sentMessage.toPrettyString());
        } catch (IOException e) {
            throw new MessagingException("mail error", e);
        }
    }

    @Override
    public void sendErrorMessage(String[] args, String errorContent) {
        Map<String, Object> model = new HashMap<>();
        model.put("operationArgs", args);
        model.put("errorContent", errorContent);
        String text = VelocityEngineUtils.mergeTemplateIntoString(
                velocityEngine, "velocity/errorMessage.mailTemplate.vm", "UTF-8", model);
    }
}
