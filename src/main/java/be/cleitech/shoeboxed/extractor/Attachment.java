package be.cleitech.shoeboxed.extractor;

import java.net.URL;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
public class Attachment {

    public String name;
    public URL url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}
