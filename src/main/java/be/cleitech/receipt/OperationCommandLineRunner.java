package be.cleitech.receipt;

import be.cleitech.receipt.tasks.ProcessToOcr;
import be.cleitech.receipt.tasks.PublishTask;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Created by ppc on 1/31/2017.
 */
@Component
public class OperationCommandLineRunner implements CommandLineRunner {

    private final ProcessToOcr processToOcr;
    private final PublishTask publishTask;
    private final DataStoreFactory dataStoreFactory;
    private final MailManager mailManager;

    @Autowired
    public OperationCommandLineRunner(ProcessToOcr processToOcr, PublishTask publishTask, DataStoreFactory dataStoreFactory, MailManager mailManager) {
        this.processToOcr = processToOcr;
        this.publishTask = publishTask;
        this.dataStoreFactory = dataStoreFactory;
        this.mailManager = mailManager;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            //TODO log this shit
            mailManager.sendErrorMessage(args,"expected process-to-ocr or extract-and-publish");
            System.exit(1);
        }
        String operation = args[0];
        switch (operation) {
            case "process-to-ocr":
                processToOcr.run(args);
                System.exit(0);
            case "extract-and-publish":
                publishTask.run(args);
                System.exit(0);
            case "test-conf":

                DataStore<StoredCredential> defaultDataStore = StoredCredential.getDefaultDataStore(dataStoreFactory);
                StoredCredential user = defaultDataStore.get("user");
                break;

            default:
                mailManager.sendErrorMessage(args,"Unknown operation " + operation);
                System.exit(1);

        }
    }
}
