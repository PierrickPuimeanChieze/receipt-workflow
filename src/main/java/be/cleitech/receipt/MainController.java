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
 * Created by pierrick on 07.02.17.
 */
@Controller
@RequestMapping("operation/")
public class MainController {

    private final PublishTask publishTask;

    private final ProcessToOcrTask processToOcrTask;
    @Autowired
    public MainController(PublishTask publishTask, ProcessToOcrTask processToOcrTask) {
        this.publishTask = publishTask;
        this.processToOcrTask = processToOcrTask;
    }

    @RequestMapping("publish/")
    public String runPublish(Model model) {
        PublishTaskResult result = publishTask.retrieveAllFile();
        model.addAttribute("result", result);
        return
                "uploadResultViewTemplate";
    }

    @RequestMapping("process/")
    public void runPublishToOcr(Model model) throws IOException, MessagingException {
        processToOcrTask.run();

    }

}
