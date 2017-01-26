package be.cleitech.shoeboxed.extractor;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.ui.velocity.VelocityEngineUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Created by ppc on 1/26/2017.
 */
public class GmailService implements MailManager {


    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();
    private final Gmail gmail;
    private VelocityEngine velocityEngine;
    private final String uploadResultDest;
    private final String uploadResultCc;
    private final String uploadResultSubject;
    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private FileDataStoreFactory DATA_STORE_FACTORY;
    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/gmail-java-quickstart");
    private static final String APPLICATION_NAME =
            "shoeboxed-toolsuite";
    private HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES =
            Arrays.asList(GmailScopes.GMAIL_SEND);
    private String uploadResultFrom;

    public GmailService(VelocityEngine velocityEngine, String uploadResultDest, String uploadResultCc, String uploadResultFrom, String uploadResultSubject) throws GeneralSecurityException, IOException {
        this.velocityEngine = velocityEngine;
        this.uploadResultDest = uploadResultDest;
        this.uploadResultCc = uploadResultCc;
        this.uploadResultFrom = uploadResultFrom;
        this.uploadResultSubject = uploadResultSubject;

        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        Credential credential = authorize();
        gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential authorize() throws IOException {

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new FileReader("./google_client_secret.json"));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
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
    public static MimeMessage createEmail(String to,
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
}
