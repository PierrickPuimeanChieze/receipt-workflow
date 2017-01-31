package be.cleitech.receipt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by ppc on 1/30/2017.
 */
@ConfigurationProperties("mail")
public class MailProperties {
    public static class MailInfo {
        public String to;
        public String cc;
        public String subject;
        public String from;

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        @Override
        public String toString() {
            return "MailInfo{" +
                    "to='" + to + '\'' +
                    ", cc='" + cc + '\'' +
                    ", subject='" + subject + '\'' +
                    ", from='" + from + '\'' +
                    '}';
        }
    }

    private MailInfo uploadResult = new MailInfo();
    private MailInfo publishOcr = new MailInfo();
    private MailInfo errorMessage = new MailInfo();

    public MailInfo getUploadResult() {
        return uploadResult;
    }

    public void setUploadResult(MailInfo uploadResult) {
        this.uploadResult = uploadResult;
    }

    public MailInfo getPublishOcr() {
        return publishOcr;
    }

    public void setPublishOcr(MailInfo publishOcr) {
        this.publishOcr = publishOcr;
    }

    public MailInfo getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(MailInfo errorMessage) {
        this.errorMessage = errorMessage;
    }
}
