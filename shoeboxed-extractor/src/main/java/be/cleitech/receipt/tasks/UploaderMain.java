package be.cleitech.receipt.tasks;

import be.cleitech.receipt.google.DriveService;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public class UploaderMain {



    private static final String TO_UPLOAD_DIR_NAME = "to_upload";

    private ShoeboxedService shoeboxedService;


    private DriveService googleDriveService;
    private String uploadedDirName;

    public UploaderMain(DriveService googleDriveService, ShoeboxedService shoeboxedService, String uploadedDirName) {
        this.googleDriveService = googleDriveService;
        this.shoeboxedService = shoeboxedService;
        this.uploadedDirName = uploadedDirName;
    }

    public void run(String[] args) throws IOException {

        // Print the names and IDs for up to 10 files.
        final String toUploadDirId = googleDriveService.retrieveFileId(TO_UPLOAD_DIR_NAME);
        final String uploadedDirId = googleDriveService.retrieveFileId(uploadedDirName);

        Set<File> fileToUpload = googleDriveService.retrieveFileToUpload(toUploadDirId);
        handleFiles(toUploadDirId, uploadedDirId, fileToUpload, shoeboxedService);

    }

    private void handleFiles(String toUploadDirId, String uploadedDirId, Collection<File> fileToUpload, ShoeboxedService shoeboxedService) throws IOException {
        if (fileToUpload == null || fileToUpload.size() == 0) {
            //TODO  LOG this shit
            System.out.println("No File to upload");
        } else {

            for (File file : fileToUpload) {
                Path tempFileName = googleDriveService.downloadTempFile(file.getId(), file.getOriginalFilename());
                shoeboxedService.uploadDocument(tempFileName);
                googleDriveService.moveFileToUploadedDir(file.getId(), toUploadDirId, uploadedDirId);
                System.out.println(file);
            }
        }
    }




}