package be.cleitech.shoeboxed.extractor;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.io.IOException;

/**
 * Created by ppc on 1/25/2017.
 */
@Configuration
@ComponentScan("be.cleitech.shoeboxed.extractor") // search the com.company package for @Component classes
//TODO manage mulitple possible path
@PropertySource(
        {"./shoeboxed-toolsuite.properties"
//                ,
//                "/etc/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
//                "~/.shoeboxed-toolsuite/shoeboxed-toolsuite.properties"
        })
public class ExtractorApplication {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ExtractorApplication.class);
        ctx.getBean(ExtractorMain.class).run(args);
    }
}
