package be.cleitech.shoeboxed.extractor;

import com.cleitech.shoeboxed.commons.ShoeboxedService;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.velocity.VelocityEngineFactoryBean;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Properties;

/**
 * Created by ppc on 1/25/2017.
 */
@Configuration
@ComponentScan("be.cleitech.shoeboxed.extractor") // search the com.company package for @Component classes
//TODO manage mulitple possible path
@PropertySource(
        {"file:./shoeboxed-toolsuite.properties"
//                ,
//                "/etc/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                "~/.shoeboxed-toolsuite/shoeboxed-toolsuite.properties"
        })
public class ExtractorApplication {

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

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ExtractorApplication.class);
        ctx.getBean(ExtractorMain.class).run(args);
//        ctx.getBean(GmailService.class).sentExtractionResults(Collections.singletonList("TEST"));
    }

    @Bean
    public ExtractorMain extractorMain() throws GeneralSecurityException, IOException {
        return new ExtractorMain(shoeboxedService(), gmailService());
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
        return new ShoeboxedService(redirectUrl, clientId);
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
        GmailService gmailService = new GmailService(velocityEngine(), uploadResultDest, uploadResultCc, uploadResultFrom, uploadResultSubject);
        return gmailService;
    }
}

