package be.cleitech.receipt.tasks;

import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import be.cleitech.receipt.shoeboxed.domain.Document;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pierrick on 07.02.17.
 */
@Component
public class ShoeboxedDocumentReader extends AbstractItemCountingItemStreamItemReader<Document> {

    private ShoeboxedService shoeboxedService;

    private LinkedList<Document> documentsToTreat;

    @Value("${shoeboxed.toSentCategory}")
    private String toSentCategory;

    @Autowired
    public ShoeboxedDocumentReader(ShoeboxedService shoeboxedService) {
        this.shoeboxedService = shoeboxedService;
    }

    @Override
    protected Document doRead() throws Exception {
        if (documentsToTreat.isEmpty()) {
            return null;
        }
        return documentsToTreat.pop();
    }

    @Override
    protected void doOpen() throws Exception {
        documentsToTreat = shoeboxedService.retrieveDocument(toSentCategory);

    }

    @Override
    protected void doClose() throws Exception {
        documentsToTreat = null;
    }


}
