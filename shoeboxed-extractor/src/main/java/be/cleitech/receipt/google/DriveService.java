package be.cleitech.receipt.google;

import be.cleitech.receipt.Utils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
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
    private final Drive drive;


    public DriveService(HttpTransport httpTransport, JsonFactory jsonFactory, HttpRequestInitializer credential, String applicationName) throws GeneralSecurityException, IOException {
        drive = new Drive.Builder(
                httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();

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
