package be.cleitech.shoeboxed.extractor;

import be.cleitech.shoeboxed.extractor.domain.Document;
import be.cleitech.shoeboxed.extractor.domain.Documents;
import be.cleitech.shoeboxed.extractor.domain.User;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.http.*;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Main extends Application {

    private static final String RESPONSE_TYPE = "token";
    private static final String SCOPE = "all";

    private Scene scene;
    private UriComponentsBuilder usersAccountUri;
    private UriComponentsBuilder documentAccountUri;

    private String toSentCategory = "to send";
    private String clientId = "4dfe5b6b1de54f9d83c24d2ea9ea76d8";
    private String[] destinationDirs = {"C:\\Users\\Pierrick\\Google Drive\\Cleitech\\Facturettes\\Test\\",};
    private String noCategoryDir = "noCategoryDir";
    private String redirectUrl = "https://api.shoeboxed.com/v2/explorer/o2c.html";
    private final int maxIndexation = 150;
    boolean exportLaunched = false;

    private  synchronized boolean isExportLaunched() {
        return exportLaunched;
    }

    private  synchronized void setExportLaunched(boolean exportLaunched) {
        this.exportLaunched = exportLaunched;
    }

    @Override
    public void init() throws Exception {
        super.init();
        Properties properties = new Properties();
        properties.load(new FileInputStream("shoeboxedExporter.properties"));
        toSentCategory = properties.getProperty("toSentCategory");
        clientId = properties.getProperty("clientId");
        destinationDirs = properties.getProperty("destinationDirs").split(";");
        noCategoryDir = properties.getProperty("noCategoryDir");
        redirectUrl = properties.getProperty("redirectUrl");
        usersAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/user/");
        documentAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/accounts/{accountId}/documents/")
                .queryParam("limit", 100)
                .queryParam("type", "receipt")
                .queryParam("category", toSentCategory)
                .queryParam("trashed", false);


    }

    @Override
    public void start(final Stage stage) throws Exception {
        final String url = UriComponentsBuilder.fromPath("https://id.shoeboxed.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", RESPONSE_TYPE)
                .queryParam("scope", SCOPE)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("state", "CRT")
                .build().toUriString();

        BorderPane borderPane = new BorderPane();

        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();

        webEngine.load(url);
//        webEngine.
        borderPane.setCenter(browser);

        webEngine.setOnStatusChanged(event -> {
            if (event.getSource() instanceof WebEngine) {
                WebEngine we = (WebEngine) event.getSource();
                String location = we.getLocation();

                if (location.startsWith(redirectUrl) && location.contains("access_token")) {
                    try {
                        URL url1 = new URL(location);
                        String[] params = url1.getRef().split("&");
                        Map<String, String> map = new HashMap<>();
                        for (String param : params) {
                            String name = param.split("=")[0];
                            String value = param.split("=")[1];
                            map.put(name, value);
                        }
                        stage.hide();
                        retrieveAllFile(map.get("access_token"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // create scene
        stage.setTitle("Skydrive");
        scene = new Scene(borderPane, 750, 500);
        stage.setScene(scene);
        stage.show();
    }

    private void retrieveAllFile(String accesToken) throws IOException {
        if (isExportLaunched()) {
            return;
        }

        setExportLaunched(true);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accesToken);
        HttpEntity entity = new HttpEntity(headers);

        final ResponseEntity<User> exchange =
                restTemplate.exchange(usersAccountUri.build().toUri(),
                        HttpMethod.GET, entity, User.class);

        final User body = exchange.getBody();
        String id = body.getAccounts()[0].getId();
        final ResponseEntity<String> debug = restTemplate.exchange(documentAccountUri.buildAndExpand(id).toUri(), HttpMethod.GET, entity, String.class);

        final ResponseEntity<Documents> documentsResponse = restTemplate.exchange(documentAccountUri.buildAndExpand(id).toUri(), HttpMethod.GET, entity, Documents.class);

        final Document[] documents = documentsResponse.getBody().getDocuments();

        for (Document document : documents) {


            try {
                String fileName = String.format("%tF_%s_%s%s.pdf",
                        document.getIssued(),
                        document.getVendor().replaceAll(" ", ""),
                        document.getTotalInPreferredCurrency().toString().replace('.', ','),
                        extractNotesInfo(document.getNotes()));
                String subDir = String.format("postDate_%tF/%s", new Date(), extractFirstDestinationCategory(document.getCategories()));

                for (String destinationDir : destinationDirs) {
                    File subDirTotal = new File(destinationDir, subDir);
                    if (!subDirTotal.exists()) {
                        subDirTotal.mkdirs();
                    }

                    File destinationFile = createDestinationFile(fileName, subDirTotal, null);
                    try (final InputStream in = document.getAttachment().getUrl().openStream();
                         final FileOutputStream out = new FileOutputStream(destinationFile)) {
                        FileCopyUtils.copy(in, out);
                    }
                    System.out.println(destinationFile.toPath());
                }

            } catch (Exception e) {
                System.err.println("Error when trying to recover document "+document);
                throw e;
            }
        }
    }

    private File createDestinationFile(String fileName, File subDirTotal, Integer index) throws IOException {
        String indexedFileName = fileName;
        int nextIndex;
        if (index != null) {
            if (index > maxIndexation) {
                throw new FileNotFoundException(fileName + " plus indexation");
            }
            int extensionsIndex = fileName.lastIndexOf(".");
            String extension = fileName.substring(extensionsIndex);

            indexedFileName = fileName.substring(0, extensionsIndex) + "_(" + index+")"+extension;
            nextIndex = index + 1;
        } else {
            nextIndex = 1;
        }
        File destinationFile = new File(subDirTotal, indexedFileName);
        if (destinationFile.exists()) {
            return createDestinationFile(fileName, subDirTotal, nextIndex);
        }
        return destinationFile;
    }

    private String extractFirstDestinationCategory(String[] categories) {
        for (String category : categories) {
            if (!category.equals(this.toSentCategory)) {
                return category;
            }
        }
        return noCategoryDir;
    }

    private String extractNotesInfo(String notes) {
        if (notes == null) {
            return "";
        }
        Properties notesP = new Properties();
        try {
            notesP.load(new StringReader(notes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String receiptType = notesP.getProperty("type");
        if (receiptType == null) {
            return "";
        }
        return "_" + receiptType;
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}