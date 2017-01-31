package be.cleitech.receipt.google;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by ppc on 1/26/2017.
 */

public class DriveService {
    private static Log LOG = LogFactory.getLog(DriveService.class);
    private final Drive drive;


    public DriveService(HttpTransport httpTransport, JsonFactory jsonFactory, HttpRequestInitializer credential, String applicationName) throws GeneralSecurityException, IOException {
        drive = new Drive.Builder(
                httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();

    }


    public String retrieveFileId(String dirName) throws IOException {
        LOG.debug("Retrieving drive id for dir "+dirName);
        FileList result = drive.files().list()
                .set("q", "trashed = false and name='" +
                        dirName +
                        "'")
                .setPageSize(10)
                .setFields("nextPageToken, files(id)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            final String message = "No dir found named" + dirName;
            throw new RuntimeException(message);
        } else if (files.size() > 1) {
            final String message = "More than on dir found named" + dirName;
            throw new RuntimeException(message);
        }
        File file = files.get(0);
        LOG.debug("Id is " + file.getId());
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

        final Drive.Files.Update update = drive.files().update(id, null);
        update.setRemoveParents(sourceDir);
        update.setAddParents(destDir);
        update.setFields("id, parents");
        final File execute = update.execute();
    }

    public Set<File> retrieveFileToUpload(String toUploadDirId) throws IOException {
        LOG.debug("Retrieve files from "+toUploadDirId);
        Set<File> wholeSet = new HashSet<>();

        FileList fileResult = drive.files().list()
                .set("q", "'" + toUploadDirId + "' in parents")
                .setPageSize(50)
                .setFields("files(id, originalFilename),nextPageToken")
                .execute();
        List<File> fileToUpload = fileResult.getFiles();
        wholeSet.addAll(fileToUpload);
        String nextPageToken = fileResult.getNextPageToken();
        LOG.debug("next Page Token : "+ nextPageToken);
        while (nextPageToken != null) {
            LOG.debug("retrieve new result");
            fileResult = drive.files().list()
                    .set("q", "trashed = false and '" + toUploadDirId + "' in parents")
                    .setPageToken(nextPageToken)
                    .setPageSize(50)
                    .setFields("files(id, originalFilename),nextPageToken")
                    .execute();
            fileToUpload = fileResult.getFiles();
            nextPageToken = fileResult.getNextPageToken();
            LOG.debug("next Page Token : "+ nextPageToken);
            wholeSet.addAll(fileToUpload);
        }
        return wholeSet;
    }
}
