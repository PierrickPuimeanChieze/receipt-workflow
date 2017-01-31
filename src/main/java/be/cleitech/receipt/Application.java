package be.cleitech.receipt;

import be.cleitech.receipt.google.GoogleConfiguration;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import be.cleitech.receipt.shoeboxed.domain.ProcessingState;
import com.dropbox.core.json.JsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.File;
import java.io.IOException;

/**
 * Created by ppc on 1/25/2017.
 */
@Import(GoogleConfiguration.class)
@SpringBootApplication
        (exclude = {EmbeddedServletContainerAutoConfiguration.class,
                WebMvcAutoConfiguration.class})
public class Application {

    final GoogleConfiguration googleConfiguration;
    //TODO add shoeboxed prefix
    @Value("${redirectUrl}")
    String redirectUrl;
    @Value("${clientId}")
    String clientId;

    @Value("${credentials.directory}/shoeboxedAccessToken")
    File shoeboxedAccesTokenTile;

    @Value("${shoeboxed.uploadProcessingState:NEEDS_SYSTEM_PROCESSING}")
    private ProcessingState shoeboxedProcessingStateForUpload;
    @Value("${processToOcr.uploadedDirName:uploaded}")
    private String shoeboxedUploadedDirName;

    @Autowired
    public Application(GoogleConfiguration googleConfiguration) {
        this.googleConfiguration = googleConfiguration;
    }


    public static void main(String[] args){
        SpringApplication.run(Application.class, args);
    }


    /**
     * Property placeholder configurer needed to process @Value annotations
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ShoeboxedService shoeboxedService() throws IOException, JsonReader.FileLoadException {
        ShoeboxedService shoeboxedService = new ShoeboxedService(redirectUrl, clientId, shoeboxedProcessingStateForUpload, shoeboxedAccesTokenTile);
        shoeboxedService.initAccessToken();
        return shoeboxedService;
    }


}

