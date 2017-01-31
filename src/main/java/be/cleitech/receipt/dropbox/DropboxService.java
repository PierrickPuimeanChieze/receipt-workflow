package be.cleitech.receipt.dropbox;

import be.cleitech.receipt.Utils;
import be.cleitech.receipt.tasks.PublishTask;
import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Created by ppc on 1/30/2017.
 */
@Component
public class DropboxService {
    private final static String[] DROPBOX_SECRET_PATHS = new String[]{
            "./dropbox_client_secret.json",
            "/etc/shoeboxed-toolsuite/dropbox_client_secret.json",
            System.getenv("APPDATA") + "/shoeboxed-toolsuite/dropbox_client_secret.json",
            "~/.shoeboxed-toolsuite/dropbox_client_secret.json"
    };
    @Value("${dropbox.uploadPath}")
    private String uploadPath;

    private String accessToken;

    private DbxAuthFinish authFinish;

    private DbxAppInfo appInfo;

    @Value("${credentials.directory}/dropboxAcessToken")
    private File accessTokenFile;

    public void initDropboxAccessToken() throws JsonReader.FileLoadException, IOException {
        if (!accessTokenFile.exists()) {
            accessToken = retrieveDropBoxAccessToken();
            try (FileWriter fileWriter = new FileWriter(accessTokenFile)) {
                fileWriter.write(accessToken);
            }

        } else {
            try (
                    FileReader fileReader = new FileReader(accessTokenFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader)
            ) {
                accessToken = bufferedReader.readLine();
            }
        }
    }

    public void uploadFile(File fileToUpload, String fileName, PublishTask publishTask) throws DbxException, IOException {
        // Create Dropbox client
        DbxRequestConfig config = new DbxRequestConfig("shoeboxed-toolsuite");
        DbxClientV2 client = new DbxClientV2(config, accessToken);

        // Upload file to Dropbox
        try (InputStream in = new FileInputStream(fileToUpload)) {
            client.files().uploadBuilder(uploadPath + "/" + fileName)
                    .uploadAndFinish(in);
        }
    }


    private String retrieveDropBoxAccessToken() throws JsonReader.FileLoadException, IOException {
        File dropBoxInfoFile = Utils.findConfFile(DROPBOX_SECRET_PATHS);
        if (dropBoxInfoFile == null) {
            throw new FileNotFoundException("Unable to found dropbox client secret file");
        }
        appInfo = DbxAppInfo.Reader.readFromFile(dropBoxInfoFile);

        DbxRequestConfig requestConfig = new DbxRequestConfig("shoeboxed-toolsuite");
        DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .build();

        String authorizeUrl = webAuth.authorize(webAuthRequest);
        System.out.println("Go to " + authorizeUrl);

        String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (code == null) {
            //FIXME : put an exception
            throw new RuntimeException("code==null");
        }
        //TODO log this shit
        System.out.println("Authorization Code :" + code);
        code = code.trim();
        try {
            authFinish = webAuth.finishFromCode(code);
        } catch (DbxException ex) {
            throw new RuntimeException("Error in DbxWebAuth.authorize: " + ex.getMessage());
        }


        System.out.println("Authorization complete.");
        System.out.println("- User ID: " + authFinish.getUserId());
        System.out.println("- Access Token: " + authFinish.getAccessToken());
        return authFinish.getAccessToken();
    }
}
