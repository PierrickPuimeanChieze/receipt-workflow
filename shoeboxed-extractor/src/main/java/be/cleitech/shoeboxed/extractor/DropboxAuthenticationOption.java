package be.cleitech.shoeboxed.extractor;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

/**
 * Created by ppc on 1/23/2017.
 */
public class DropboxAuthenticationOption extends RuntimeException {
    private final JBrowserDriver driver;

    public DropboxAuthenticationOption(String s, JBrowserDriver driver)  {
        super(s);
        this.driver = driver;
    }
}
