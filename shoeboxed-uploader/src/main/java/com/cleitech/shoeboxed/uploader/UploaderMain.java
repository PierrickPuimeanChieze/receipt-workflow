package com.cleitech.shoeboxed.uploader;

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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UploaderMain {
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
            Arrays.asList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String TO_UPLOAD_DIR_NAME = "to_upload";

    private static Drive service;

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
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                UploaderMain.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

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
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {

        // Build a new authorized API client service.
        service = getDriveService();
        // Print the names and IDs for up to 10 files.
        final String toUploadDirId = retrieveFileId(TO_UPLOAD_DIR_NAME);
        final String uploadedDirId = retrieveFileId(UPLOADED_DIR_NAME);


        FileList fileResult = service.files().list()
                .set("q", "'" + toUploadDirId + "' in parents")
                .setPageSize(1)
                .setFields("files(id, originalFilename),nextPageToken")
                .execute();
        List<File> fileToUpload = fileResult.getFiles();
        String nextPageToken = fileResult.getNextPageToken();
        System.out.printf("newxt Page Token : %s\n", nextPageToken);
        handleFiles(uploadedDirId, fileToUpload);
        while (nextPageToken != null) {
            System.out.println("retrieve new result");
            fileResult = service.files().list()
                    .set("q", "'" + toUploadDirId + "' in parents")
                    .setPageToken(nextPageToken)
                    .setPageSize(1)
                    .setFields("files(id, originalFilename),nextPageToken")
                    .execute();
            fileToUpload = fileResult.getFiles();
            nextPageToken = fileResult.getNextPageToken();
            System.out.printf("newxt Page Token : %s\n", nextPageToken);
            handleFiles(uploadedDirId, fileToUpload);
        }

    }

    private static void handleFiles(String uploadedDirId, List<File> fileToUpload) throws IOException {
        if (fileToUpload == null || fileToUpload.size() == 0) {
            //TODO  LOG this shit
            System.out.println("No File to upload");
        } else {

            for (File file : fileToUpload) {
                Path tempFileName = downloadTempFile(file);
                uploadFileToShoeboxed(tempFileName);
                moveFileToUploadedDir(file.getId(), uploadedDirId);
                System.out.println(file);
            }
        }
    }

    private static void moveFileToUploadedDir(String id, String uploadedDirId) {
        System.out.println("move file "+id+" to dir "+uploadedDirId);
    }

    private static void uploadFileToShoeboxed(Path tempFileName) {
        //TODO LOG this shit
        System.out.println("upload "+tempFileName+" to shoeboxed");


        final java.io.File file = tempFileName.toFile();
        Resource resourceToUpload = new FileSystemResource(file);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("attachment", resourceToUpload);
        body.add("document", "{ \"processingState\": \"PROCESSED\", \"type\":\"receipt\"}");
        HttpEntity entity = new HttpEntity<>(body, headers);
        final ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(url, entity, String.class);
        final HttpStatus statusCode = stringResponseEntity.getStatusCode();
        System.out.println(stringResponseEntity.getBody());

        if (statusCode == HttpStatus.CREATED) {
            System.out.println(path +" uploaded. Moving it");

            final Path dest = uploadedDir.resolve(path.getFileName());
            try {
                Files.move(path, dest);
            } catch (IOException e) {
                System.err.println("unable to move " + path + " to " + dest + " after upload");
                e.printStackTrace();
            }
        }
    }

    private static String retrieveFileId(String dirName) throws IOException {
        FileList result = service.files().list()
                .set("q", "name='" +
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
        return files.get(0).getId();
    }

    private static Path downloadTempFile(File file) throws IOException {
        final String fileId = file.getId();
        Path tempFileName = Files.createTempFile("shoeb_", file.getOriginalFilename());
        OutputStream outputStream = new FileOutputStream(tempFileName.toFile());
        service.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        outputStream.close();
        return tempFileName;
    }

    private void startUploadingProcess(String accessToken, Path toUploadDir, Path uploadedDir) {


        try {
            Files.list(toUploadDir)
                    .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                    .forEach(path -> {

                            }
                    );
        } catch (IOException e) {
            System.err.println("unable to list files of " + toUploadDir);
            e.printStackTrace();
        }
    }


}