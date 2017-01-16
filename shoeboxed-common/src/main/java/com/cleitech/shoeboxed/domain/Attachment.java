package com.cleitech.shoeboxed.domain;

import java.net.URL;

/**
 * @author Pierrick Puimean-Chieze on 23-04-16.
 */
public class Attachment {

    private String name;
    private  URL url;

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

    @Override
    public String toString() {
        return "Attachment{" +
                "name='" + name + '\'' +
                ", url=" + url +
                '}';
    }
}
