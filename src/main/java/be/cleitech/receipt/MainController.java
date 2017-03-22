package be.cleitech.receipt;

import be.cleitech.receipt.tasks.PublishTask;
import be.cleitech.receipt.tasks.PublishTaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by pierrick on 07.02.17.
 */
@Controller
@RequestMapping("operation/")
public class MainController {

    private final PublishTask publishTask;

    @Autowired
    public MainController(PublishTask publishTask) {
        this.publishTask = publishTask;
    }

    @RequestMapping("publish/")
    public String runPublish(Model model) {
        PublishTaskResult result = publishTask.retrieveAllFile();
        model.addAttribute("result", result);
        return
                "uploadResultViewTemplate";
    }
}
