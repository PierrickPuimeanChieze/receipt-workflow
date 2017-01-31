package be.cleitech.receipt.tasks;

import be.cleitech.receipt.MailManager;
import be.cleitech.receipt.google.DriveService;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Component
public class ProcessToOcr {


    private static final String TO_UPLOAD_DIR_NAME = "to_upload";

    private ShoeboxedService shoeboxedService;
    private MailManager mailManager;

    private DriveService googleDriveService;

    @Value("${processToOcr.uploadedDirName:uploaded}")
    private String uploadedDirName;

    @Autowired
    public ProcessToOcr(
            DriveService googleDriveService,
            ShoeboxedService shoeboxedService,
            MailManager mailManager) {
        this.googleDriveService = googleDriveService;
        this.shoeboxedService = shoeboxedService;
        this.mailManager = mailManager;
    }

    public void run(String[] args) throws IOException, MessagingException {

        // Print the names and IDs for up to 10 files.
        final String toUploadDirId = googleDriveService.retrieveFileId(TO_UPLOAD_DIR_NAME);
        final String uploadedDirId = googleDriveService.retrieveFileId(uploadedDirName);

        Set<File> fileToUpload = googleDriveService.retrieveFileToUpload(toUploadDirId);
        handleFiles(toUploadDirId, uploadedDirId, fileToUpload, shoeboxedService);

    }

    private void handleFiles(String toUploadDirId, String uploadedDirId, Collection<File> fileToUpload, ShoeboxedService shoeboxedService) throws IOException, MessagingException {

        if (fileToUpload == null || fileToUpload.size() == 0) {
            //TODO  LOG this shit
            System.out.println("No File to upload");
        } else {
            Collection<String> publishedFile = new ArrayList<>();
            for (File file : fileToUpload) {
                Path tempFileName = googleDriveService.downloadTempFile(file.getId(), file.getOriginalFilename());
                shoeboxedService.uploadDocument(tempFileName);
                googleDriveService.moveFileToUploadedDir(file.getId(), toUploadDirId, uploadedDirId);
                publishedFile.add(file.getOriginalFilename());
            }
            mailManager.sentPublishOcrProcess(publishedFile);
        }
    }


}