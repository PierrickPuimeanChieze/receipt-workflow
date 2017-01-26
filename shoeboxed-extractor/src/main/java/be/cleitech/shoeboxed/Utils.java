package be.cleitech.shoeboxed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by ppc on 1/20/2017.
 */
public class Utils {
    private static final String[] SHOEBOXED_TOOLSUITE_PROPERTIES_PATHS = new String[]{
            "./shoeboxed-toolsuite.properties",
            "/etc/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
            System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxed-toolsuite.properties",
            "~/.shoeboxed-toolsuite/shoeboxed-toolsuite.properties"
    };
    private static Properties loadedProperties = null;

    public static java.io.File findConfFile(String[] pathsToLook) {
        for (String clientSecretPath : pathsToLook) {
            java.io.File file = new java.io.File(clientSecretPath);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static Properties getConfProperties() throws IOException {
        if (loadedProperties == null) {
            File confFile = findConfFile(SHOEBOXED_TOOLSUITE_PROPERTIES_PATHS);
            if (confFile == null) {
                throw new FileNotFoundException("shoeboxed-toolsuite.properties in any of the default Path");
            }
            loadedProperties = new Properties();
            loadedProperties.load(new FileInputStream(confFile));
        }
        return loadedProperties;
    }
}
