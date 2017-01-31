package be.cleitech.receipt.google;

import be.cleitech.receipt.MailProperties;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.GmailScopes;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ppc on 1/26/2017.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class GoogleConfiguration {

    @Value("${credentials.directory}")
    File credentialDirectory;

    final MailProperties mailProperties;

    @Value("${spring.application.name")
    private String applicationName;

    final VelocityEngine velocityEngine;

    @Autowired
    public GoogleConfiguration(MailProperties mailProperties, VelocityEngine velocityEngine) {
        this.mailProperties = mailProperties;
        this.velocityEngine = velocityEngine;
    }

    @Bean
    public GmailService gmailService() throws GeneralSecurityException, IOException {
        return new GmailService(httpTransport(), googleCredentials(), jsonFactory(),
                velocityEngine, applicationName, mailProperties);
    }

    private HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();

    }

    @Bean
    public DriveService driveService() throws GeneralSecurityException, IOException {
        return new DriveService(httpTransport(), jsonFactory(), googleCredentials(), applicationName);
    }

    @Bean
    public DataStoreFactory googleCredentialsDataStoreFactory() throws IOException {

        return new FileDataStoreFactory(credentialDirectory);

    }

    private List<String> googleAuthorizeScopes() {
        return Arrays.asList(GmailScopes.GMAIL_SEND,
                DriveScopes.DRIVE_METADATA_READONLY,
                DriveScopes.DRIVE);
    }

    private Credential googleCredentials() throws IOException, GeneralSecurityException {

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(jsonFactory(), new FileReader("./google_client_secret.json"));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport(), jsonFactory(), clientSecrets, googleAuthorizeScopes())
                        .setDataStoreFactory(googleCredentialsDataStoreFactory())
                        .setAccessType("offline")
                        .build();
        return new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
    }

    @Bean
    public JsonFactory jsonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

}
