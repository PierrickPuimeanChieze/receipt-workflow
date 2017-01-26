package be.cleitech.shoeboxed.domain;

import java.util.Arrays;
import java.util.Date;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */

public class Document {


    private Attachment attachment;
    private Double total;
    private Double tax;
    private String currency;
    private Date issued;
    private Date uploaded ;
    private String vendor;
    private String notes;
    private String[] categories;

    private String id;
    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Date getIssued() {
        return issued;
    }

    public void setIssued(Date issued) {
        this.issued = issued;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getUploaded() {
        return uploaded;
    }

    public void setUploaded(Date uploaded) {
        this.uploaded = uploaded;
    }

    @Override
    public String toString() {
        return "Document{" +
                "attachment=" + attachment +
                ", total=" + total +
                ", issued=" + issued +
                ", uploaded =" + uploaded  +
                ", vendor='" + vendor + '\'' +
                ", notes='" + notes + '\'' +
                ", categories=" + Arrays.toString(categories) +
                ", id='" + id + '\'' +
                '}';
    }
}
