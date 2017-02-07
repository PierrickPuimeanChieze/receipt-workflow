package be.cleitech.receipt.shoeboxed.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {


    private Attachment attachment;
    private Double total;
    private Double tax;
    private String currency;
    private Date issued;
    private Date uploaded ;
    private String vendor;
    private String notes;
    private List<String> categories;
    private String type;

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

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
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
                ", tax=" + tax +
                ", currency='" + currency + '\'' +
                ", issued=" + issued +
                ", uploaded=" + uploaded +
                ", vendor='" + vendor + '\'' +
                ", notes='" + notes + '\'' +
                ", categories=" + categories +
                ", id='" + id + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
