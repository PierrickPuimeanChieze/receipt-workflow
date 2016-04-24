package be.cleitech.shoeboxed.extractor;

import lombok.Data;

import java.util.Date;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
@Data
public class Document {


    Attachment attachment;
    Double totalInPreferredCurrency;
    Date issued;
    String vendor;
    String notes;
    String[] categories;
}
