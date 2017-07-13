package be.cleitech.receipt.tasks;

import java.nio.file.Path;

public class ProcessTaskResult {
    private String fileStoreName;
    private Path tempFileName;
    private String errorMessage;

    public void setFileStoreName(String fileStoreName) {
        this.fileStoreName = fileStoreName;
    }

    public String getFileStoreName() {
        return fileStoreName;
    }

    public void setTempFileName(Path tempFileName) {
        this.tempFileName = tempFileName;
    }

    public Path getTempFileName() {
        return tempFileName;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isInError() {
        return errorMessage != null;
    }
}
