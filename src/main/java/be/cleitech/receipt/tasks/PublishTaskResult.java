package be.cleitech.receipt.tasks;

import be.cleitech.receipt.shoeboxed.domain.Document;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pierrick on 07.02.17.
 */
public class PublishTaskResult {

    private boolean mailSent;

    public LinkedList<String> getUploadedFile() {
        return uploadedFile;
    }

    private LinkedList<String> uploadedFile = new LinkedList<>();
    private LinkedList<String> errorFile = new LinkedList<>();
    private List<Document> documentsToProceed;

    public void setDocumentsToProceed(List<Document> documentsToProceed) {
        this.documentsToProceed = documentsToProceed;
    }

    public List<Document> getDocumentsToProceed() {
        return documentsToProceed;
    }


    public void addFailedDocument(Document document, String fileName) {
        errorFile.add(fileName);
    }

    public void addSuccessDocument(Document document, String fileName) {
        uploadedFile.add(fileName);
    }

    public void setMailSent(boolean mailSent) {
        this.mailSent = mailSent;
    }

    public LinkedList<String> getErrorFile() {
        return errorFile;
    }

    public boolean isMailSent() {
        return mailSent;
    }
}
