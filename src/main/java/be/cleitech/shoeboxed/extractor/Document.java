package be.cleitech.shoeboxed.extractor;

import lombok.Data;

import java.util.Date;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
public class Document {


    Attachment attachment;
    Double totalInPreferredCurrency;
    Date issued;
    String vendor;
    String notes;
    String[] categories;

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    public Double getTotalInPreferredCurrency() {
        return totalInPreferredCurrency;
    }

    public void setTotalInPreferredCurrency(Double totalInPreferredCurrency) {
        this.totalInPreferredCurrency = totalInPreferredCurrency;
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
}
