package be.cleitech.receipt.tasks;

import be.cleitech.receipt.MailManager;
import be.cleitech.receipt.google.DriveService;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import com.google.api.services.drive.model.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Component
public class ProcessToOcrTask {

    private static Log LOG = LogFactory.getLog(ProcessToOcrTask.class);
    @Value("${processToOcr.toUploadDirName:to_upload}")
    private String toUploadDirName;

    private ShoeboxedService shoeboxedService;
    private MailManager mailManager;

    private DriveService googleDriveService;

    @Value("${processToOcr.uploadedDirName:uploaded}")
    private String uploadedDirName;

    @Autowired
    public ProcessToOcrTask(
            DriveService googleDriveService,
            ShoeboxedService shoeboxedService,
            MailManager mailManager) {
        this.googleDriveService = googleDriveService;
        this.shoeboxedService = shoeboxedService;
        this.mailManager = mailManager;
    }

    @Scheduled(cron = "${processToOcr.schedule:0 0 0 * * ?}")
    public void run() throws IOException, MessagingException {

        final String toUploadDirId = googleDriveService.retrieveFileId(toUploadDirName);
        final String uploadedDirId = googleDriveService.retrieveFileId(uploadedDirName);

        Set<File> fileToUpload = googleDriveService.retrieveFileToUpload(toUploadDirId);
        handleFiles(toUploadDirId, uploadedDirId, fileToUpload, shoeboxedService);

    }

    private void handleFiles(String toUploadDirId, String uploadedDirId, Collection<File> fileToUpload, ShoeboxedService shoeboxedService) throws IOException, MessagingException {

        if (fileToUpload == null || fileToUpload.size() == 0) {
            LOG.info("No file to sent to OCR");
            mailManager.sentPublishOcrProcess(null);
        } else {
            Collection<String> publishedFile = new ArrayList<>();
            for (File file : fileToUpload) {
                Path tempFileName = googleDriveService.downloadTempFile(file.getId(), file.getOriginalFilename());
                LOG.info("upload " + tempFileName + " to shoeboxed");
                shoeboxedService.uploadDocument(tempFileName);
                LOG.info("move file " + file.getOriginalFilename() + " from "+ toUploadDirName +"to dir " + uploadedDirName);
                googleDriveService.moveFileToUploadedDir(file.getId(), toUploadDirId, uploadedDirId);
                publishedFile.add(file.getOriginalFilename());
            }
            mailManager.sentPublishOcrProcess(publishedFile);
        }
    }


}