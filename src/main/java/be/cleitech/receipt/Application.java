package be.cleitech.receipt;

import be.cleitech.receipt.google.GoogleConfiguration;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import be.cleitech.receipt.shoeboxed.domain.ProcessingState;
import be.cleitech.receipt.tasks.ProcessToOcr;
import be.cleitech.receipt.tasks.PublishTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by ppc on 1/25/2017.
 */
//TODO manage mulitple possible path
@Import(GoogleConfiguration.class)
@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class,
        WebMvcAutoConfiguration.class})
public class Application {

    @Autowired
    GoogleConfiguration googleConfiguration;
    //TODO add shoeboxed prefix
    @Value("${redirectUrl}")
    String redirectUrl;
    @Value("${clientId}")
    String clientId;


    @Value("${shoeboxed.uploadProcessingState:NEEDS_SYSTEM_PROCESSING}")
    private ProcessingState shoeboxedProcessingStateForUpload;
    @Value("${processToOcr.uploadedDirName:uploaded}")
    private String shoeboxedUploadedDirName;

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class);
        if (args.length < 1) {
            System.err.println("expected process-to-ocr or extract-and-send");
            System.exit(1);
        }
        String operation = args[0];
        switch (operation) {
            case "process-to-ocr":
                ctx.getBean(ProcessToOcr.class).run(args);
                System.exit(0);
            case "extract-and-send":
                ctx.getBean(PublishTask.class).run(args);
            default:
                System.err.println("Unknown operation " + operation);
                System.exit(1);

        }

    }

    @Bean
    public PublishTask extractorMain() throws GeneralSecurityException, IOException {
        return new PublishTask(shoeboxedService(), googleConfiguration.gmailService());
    }

    /**
     * Property placeholder configurer needed to process @Value annotations
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ShoeboxedService shoeboxedService() {
        ShoeboxedService shoeboxedService = new ShoeboxedService(redirectUrl, clientId, shoeboxedProcessingStateForUpload);
        shoeboxedService.authorize();
        return shoeboxedService;
    }




    @Bean
    public ProcessToOcr uploaderMain() throws GeneralSecurityException, IOException {
        return new ProcessToOcr(googleConfiguration.driveService(), shoeboxedService(), shoeboxedUploadedDirName);
    }
}

