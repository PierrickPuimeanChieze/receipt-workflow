package be.cleitech.receipt;

import be.cleitech.receipt.google.GmailService;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import be.cleitech.receipt.shoeboxed.domain.ProcessingState;
import be.cleitech.receipt.google.DriveService;
import be.cleitech.receipt.tasks.PublishTask;
import be.cleitech.receipt.tasks.ProcessToOcr;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * Created by ppc on 1/25/2017.
 */
@Configuration
//TODO manage mulitple possible path
@PropertySource(
        {"file:./shoeboxed-toolsuite.properties"
//                ,
//                "/etc/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                "~/.shoeboxed-toolsuite/shoeboxed-toolsuite.properties"
        })
public class Application {

    //TODO add shoeboxed prefix
    @Value("${redirectUrl}")
    String redirectUrl;
    @Value("${clientId}")
    String clientId;

    @Value("${mail.uploadResult.to}")
    String uploadResultDest;
    @Value("${mail.uploadResult.cc}")
    String uploadResultCc;
    @Value("${mail.uploadResult.subject}")
    private String uploadResultSubject;
    @Value("${mail.uploadResult.from}")
    private String uploadResultFrom;

    @Value("${shoeboxed.uploadProcessingState:NEEDS_SYSTEM_PROCESSING}")
    private ProcessingState shoeboxedProcessingStateForUpload;
    @Value("${shoeboxed.uploadedDirName:uploaded}")
    private String shoeboxedUploadedDirName;

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class);
        if (args.length < 1) {
            System.err.println("expected process-to-ocr or extract-and-send");
        }
        String operation = args[0];
        switch (operation) {
            case "process-to-ocr":
                ctx.getBean(ProcessToOcr.class).run(args);
                break;
            case "extract-and-send":
                ctx.getBean(PublishTask.class).run(args);
                break;
            default:
                System.err.println("Unknown operation " + operation);

        }
    }

    @Bean
    public PublishTask extractorMain() throws GeneralSecurityException, IOException {
        return new PublishTask(shoeboxedService(), gmailService());
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
    public VelocityEngine velocityEngine() throws IOException {
        VelocityEngineFactoryBean factory = new VelocityEngineFactoryBean();
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader." +
                        "ClasspathResourceLoader");
        factory.setVelocityProperties(props);

        return factory.createVelocityEngine();
    }

    @Bean
    public SimpleMailMessage uploadResultTemplateMessage() {
        SimpleMailMessage uploadTemplateMessage = new SimpleMailMessage();
        uploadTemplateMessage.setTo(uploadResultDest);
        uploadTemplateMessage.setCc(uploadResultCc);
        uploadTemplateMessage.setSubject(uploadResultSubject);
        return uploadTemplateMessage;
    }

    @Bean
    public GmailService gmailService() throws GeneralSecurityException, IOException {
        return new GmailService(velocityEngine(), uploadResultDest, uploadResultCc, uploadResultFrom, uploadResultSubject);
    }

    @Bean
    public DriveService driveService() throws GeneralSecurityException, IOException {
        return new DriveService();
    }

    @Bean
    public ProcessToOcr uploaderMain() throws GeneralSecurityException, IOException {
        return new ProcessToOcr(driveService(), shoeboxedService(), shoeboxedUploadedDirName);
    }
}

