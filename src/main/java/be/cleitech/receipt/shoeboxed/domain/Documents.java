package be.cleitech.receipt.shoeboxed.domain;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
public class Documents {

    private LinkedList<Document> documents = new LinkedList<>();

    public LinkedList<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents.clear();
        this.documents.addAll(documents);
    }
}
