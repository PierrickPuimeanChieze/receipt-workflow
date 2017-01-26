package be.cleitech.receipt.google;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by ppc on 1/26/2017.
 */
@Configuration
@PropertySource(
        {"file:./shoeboxed-toolsuite.properties"
//                ,
//                "/etc/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                "~/.shoeboxed-toolsuite/shoeboxed-toolsuite.properties"
        })
public class GoogleConfiguration {
    @Value("${mail.uploadResult.to}")
    String uploadResultDest;
    @Value("${mail.uploadResult.cc}")
    String uploadResultCc;
    @Value("${mail.uploadResult.subject}")
    private String uploadResultSubject;
    @Value("${mail.uploadResult.from}")
    private String uploadResultFrom;
    private String applicationName = "receipt-toolsuite";

    @Bean
    public GmailService gmailService() throws GeneralSecurityException, IOException {
        return new GmailService(httpTransport(), authorize(), jsonFactory(),
                velocityEngine(), applicationName, uploadResultDest, uploadResultCc, uploadResultFrom, uploadResultSubject);
    }

    private HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();

    }

    @Bean
    public DriveService driveService() throws GeneralSecurityException, IOException {
        return new DriveService(httpTransport(), jsonFactory(), authorize(), applicationName);
    }

    @Bean
    public VelocityEngine velocityEngine() throws IOException {
        VelocityEngineFactoryBean factory = new VelocityEngineFactoryBean();
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader." +
                        "ClasspathResourceLoader");
        factory.setVelocityProperties(props);

        return factory.createVelocityEngine();
    }

    @Bean
    public DataStoreFactory googleCredentialsDataStoreFactory() throws IOException {

        return new FileDataStoreFactory(new java.io.File(
                System.getProperty("user.home"), ".credentials/receipt-toolsuite/"));

    }

    private List<String> googleAuthorizeScopes() {
        return Arrays.asList(GmailScopes.GMAIL_SEND,
                DriveScopes.DRIVE_METADATA_READONLY,
                DriveScopes.DRIVE);
    }

    private Credential authorize() throws IOException, GeneralSecurityException {

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
