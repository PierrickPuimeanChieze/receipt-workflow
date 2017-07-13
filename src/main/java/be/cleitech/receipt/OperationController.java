package be.cleitech.receipt;

import be.cleitech.receipt.tasks.ProcessToOcrTask;
import be.cleitech.receipt.tasks.PublishTask;
import be.cleitech.receipt.tasks.PublishTaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * This MVC controller provide access to operation. Each operation, in addition of doing its work and showing the
 * results in the return page, will also sent with the same info to wanted parties
 *
 * @author pierrick on 07.02.17.
 */
@Controller
@RequestMapping("operation")
public class OperationController {

    private final PublishTask publishTask;

    private final ProcessToOcrTask processToOcrTask;

    @Autowired
    public OperationController(PublishTask publishTask, ProcessToOcrTask processToOcrTask) {
        this.publishTask = publishTask;
        this.processToOcrTask = processToOcrTask;
    }

    /**
     * This operation extract the file from shoeboxed, send them to Dropbox.
     *
     * @param model
     * @return
     */
    @RequestMapping("/publish")
    public String runPublish(Model model) {
        PublishTaskResult result = publishTask.retrieveAllFile();
        model.addAttribute("result", result);
        return
                "uploadResultViewTemplate";
    }

    /**
     * This operation retrieve the file from Google Drive, and send them to shoeboxed
     *
     * @param model To define to show the result of the process
     * @throws IOException
     * @throws MessagingException
     */
    @RequestMapping("/process")
    public void runPublishToOcr(Model model) throws IOException, MessagingException {
        processToOcrTask.run();

    }

}
