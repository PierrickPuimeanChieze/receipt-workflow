package be.cleitech.receipt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by ppc on 1/30/2017.
 */
@ConfigurationProperties("mail")
public class MailProperties {
    public static class MailInfo {
        public String dest;
        public String cc;
        public String subject;
        public String from;
    }

    public MailInfo uploadResult;
    public MailInfo publishOcr;
    public MailInfo errorMessage;


}
