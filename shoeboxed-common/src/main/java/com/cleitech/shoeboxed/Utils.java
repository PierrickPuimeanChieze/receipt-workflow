package com.cleitech.shoeboxed;

/**
 * Created by ppc on 1/20/2017.
 */
public class Utils {
    public static java.io.File findConfFile(String[] pathsToLook) {
        for (String clientSecretPath : pathsToLook) {
            java.io.File file = new java.io.File(clientSecretPath);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
}
