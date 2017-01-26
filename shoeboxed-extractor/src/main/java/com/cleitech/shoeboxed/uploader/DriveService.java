package com.cleitech.shoeboxed.uploader;

import be.cleitech.shoeboxed.Utils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ppc on 1/26/2017.
 */
public class DriveService {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME =
            "Drive API Java Quickstart";

    private final static String[] GOOGLE_CLIENT_SECRET_PATHS = new String[]{
            "./google_client_secret.json"
    };

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();
    private final Drive drive;

    /**
     * Global instance of the HTTP transport.
     */
    private HttpTransport HTTP_TRANSPORT;
    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);


    public DriveService() throws GeneralSecurityException, IOException {
        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        Credential credential = authorize();
        drive = new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    private Credential authorize() throws IOException {
        // Load client secrets.
        java.io.File s = Utils.findConfFile(GOOGLE_CLIENT_SECRET_PATHS);
        if (s == null) {
            throw new IOException("Unable to find client_secret.json in any of the default locations");
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new FileReader(s));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setScopes(Collections.singletonList("https://www.googleapis.com/auth/drive"))
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    public String retrieveFileId(String dirName) throws IOException {
        FileList result = drive.files().list()
                .set("q", "trashed = false and name='" +
                        dirName +
                        "'")
                .setPageSize(10)
                .setFields("nextPageToken, files(id)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            //TODO Log this shit
            final String message = "No dir found named" + dirName;
            throw new RuntimeException(message);
        } else if (files.size() > 1) {
            //TODO log this shit
            final String message = "More than on dir found named" + dirName;
            throw new RuntimeException(message);
        }
        File file = files.get(0);
        System.out.println("FULL FILE EXTENSION:" + file.getFullFileExtension());
        System.out.println("NAME:" + file.getName());
        return file.getId();
    }


    public Path downloadTempFile(String fileId, String fileName) throws IOException {
        Path tempFileName = Files.createTempFile("shoeb_", fileName);
        OutputStream outputStream = new FileOutputStream(tempFileName.toFile());
        drive.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return tempFileName;
    }

    public void moveFileToUploadedDir(String id, String sourceDir, String destDir) throws IOException {
        System.out.println("move file " + id + " from to dir " + destDir);
        final Drive.Files.Update update = drive.files().update(id, null);
        update.setRemoveParents(sourceDir);
        update.setAddParents(destDir);
        update.setFields("id, parents");
        final File execute = update.execute();
    }

    public Set<File> retrieveFileToUpload(String toUploadDirId) throws IOException {
        Set<File> wholeSet = new HashSet<>();

        FileList fileResult = drive.files().list()
                .set("q", "'" + toUploadDirId + "' in parents")
                .setPageSize(1)
                .setFields("files(id, originalFilename),nextPageToken")
                .execute();
        List<File> fileToUpload = fileResult.getFiles();
        wholeSet.addAll(fileToUpload);
        String nextPageToken = fileResult.getNextPageToken();
        System.out.printf("newxt Page Token : %s\n", nextPageToken);
        while (nextPageToken != null) {
            System.out.println("retrieve new result");
            fileResult = drive.files().list()
                    .set("q", "trashed = false and '" + toUploadDirId + "' in parents")
                    .setPageToken(nextPageToken)
                    .setPageSize(1)
                    .setFields("files(id, originalFilename),nextPageToken")
                    .execute();
            fileToUpload = fileResult.getFiles();
            nextPageToken = fileResult.getNextPageToken();
            System.out.printf("newxt Page Token : %s\n", nextPageToken);
            wholeSet.addAll(fileToUpload);
        }
        return wholeSet;
    }
}
