package com.cleitech.shoeboxed.uploader;

import com.cleitech.shoeboxed.Utils;
import com.cleitech.shoeboxed.commons.ShoeboxedService;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class UploaderMain {

    private final static String[] GOOGLE_CLIENT_SECRET_PATHS = new String[]{
            "./client_secret.json",
            "/etc/shoeboxed-toolsuite/client_secret.json",
            System.getenv("APPDATA") + "/shoeboxed-toolsuite/client_secret.json",
            "~/.shoeboxed-toolsuite/client_secret.json"
    };


    /**
     * Application name.
     */
    private static final String APPLICATION_NAME =
            "Drive API Java Quickstart";

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");
    private static final String UPLOADED_DIR_NAME = "uploaded";

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    private static String shoeboxedAccountId = "1809343498";

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String TO_UPLOAD_DIR_NAME = "to_upload";

    private static Drive googleDriveService;
    private static ShoeboxedService shoeboxedService;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    private static Credential authorize() throws IOException {
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

    /**
     * Build and return an authorized Drive client googleDriveService.
     *
     * @return an authorized Drive client googleDriveService
     * @throws IOException
     */
    private static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static ShoeboxedService getShoeboxedService() throws IOException {
        shoeboxedService = ShoeboxedService.createFromDefaultConfFilePath();
        shoeboxedService.authorize();
        return shoeboxedService;
    }


    public static void main(String[] args) throws IOException {

        for (String arg : args) {
            if (arg.equals("--version")) {
                System.out.println("YOLO");
                return;
            }

            if (arg.equals("--test-client-secret")) {
                System.out.println(Utils.findConfFile(GOOGLE_CLIENT_SECRET_PATHS));
                return;
            }
        }
        // Build a new authorized API client googleDriveService.
        googleDriveService = getDriveService();
        shoeboxedService = getShoeboxedService();

        // Print the names and IDs for up to 10 files.
        final String toUploadDirId = retrieveFileId(TO_UPLOAD_DIR_NAME);
        final String uploadedDirId = retrieveFileId(UPLOADED_DIR_NAME);


        FileList fileResult = googleDriveService.files().list()
                .set("q", "'" + toUploadDirId + "' in parents")
                .setPageSize(1)
                .setFields("files(id, originalFilename),nextPageToken")
                .execute();
        List<File> fileToUpload = fileResult.getFiles();
        String nextPageToken = fileResult.getNextPageToken();
        System.out.printf("newxt Page Token : %s\n", nextPageToken);
        handleFiles(toUploadDirId,uploadedDirId, fileToUpload, shoeboxedService);
        while (nextPageToken != null) {
            System.out.println("retrieve new result");
            fileResult = googleDriveService.files().list()
                    .set("q", "trashed = false and '" + toUploadDirId + "' in parents")
                    .setPageToken(nextPageToken)
                    .setPageSize(1)
                    .setFields("files(id, originalFilename),nextPageToken")
                    .execute();
            fileToUpload = fileResult.getFiles();
            nextPageToken = fileResult.getNextPageToken();
            System.out.printf("newxt Page Token : %s\n", nextPageToken);
            handleFiles(toUploadDirId, uploadedDirId, fileToUpload, shoeboxedService);
        }

    }

    private static void handleFiles(String toUploadDirId, String uploadedDirId, List<File> fileToUpload, ShoeboxedService shoeboxedService) throws IOException {
        if (fileToUpload == null || fileToUpload.size() == 0) {
            //TODO  LOG this shit
            System.out.println("No File to upload");
        } else {

            for (File file : fileToUpload) {
                Path tempFileName = downloadTempFile(file);
                shoeboxedService.uploadDocument(tempFileName);
                moveFileToUploadedDir(file.getId(),toUploadDirId, uploadedDirId);
                System.out.println(file);
            }
        }
    }

    private static void moveFileToUploadedDir(String id, String sourceDir, String destDir) throws IOException {
        System.out.println("move file "+id+" from to dir "+destDir);
        final Drive.Files.Update update = googleDriveService.files().update(id, null);
        update.setRemoveParents(sourceDir);
        update.setAddParents(destDir);
        update.setFields("id, parents");
        final File execute = update.execute();
    }

    private static String retrieveFileId(String dirName) throws IOException {
        FileList result = googleDriveService.files().list()
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

    private static Path downloadTempFile(File file) throws IOException {
        final String fileId = file.getId();
        Path tempFileName = Files.createTempFile("shoeb_", file.getOriginalFilename());
        OutputStream outputStream = new FileOutputStream(tempFileName.toFile());
        googleDriveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return tempFileName;
    }

}