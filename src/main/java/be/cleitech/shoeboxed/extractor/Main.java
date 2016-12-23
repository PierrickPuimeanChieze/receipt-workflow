package be.cleitech.shoeboxed.extractor;

import be.cleitech.shoeboxed.extractor.domain.Document;
import be.cleitech.shoeboxed.extractor.domain.Documents;
import be.cleitech.shoeboxed.extractor.domain.User;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
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
    /**
     * The "main" categories
     **/
    private String[] mainCategories = new String[]{"Diving"};
    private final int maxIndexation = 150;
    private boolean exportLaunched = false;

    private ArrayList<String> fileList = new ArrayList<>();

    private synchronized boolean isExportLaunched() {
        return exportLaunched;
    }

    private synchronized void setExportLaunched(boolean exportLaunched) {
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
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            int succededCount = 0;

            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    succededCount++;
                    switch (succededCount) {
                        case 1:
                            //First screen : we run the auto log
                            String loginScript = loadScript("loginScript.js");
                            webEngine.executeScript(loginScript);
                            break;
                        case 2:
                            String autoApproveScript = loadScript("autoApprove.js");
                            webEngine.executeScript(autoApproveScript);
                            break;
                    }

                }
            }
        });
        webEngine.load(url);
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

    /**
     * This method load the script content, provided the name of the file
     *
     * @param jsFile name of the file
     * @return content of the script
     */
    private String loadScript(String jsFile) {
        final InputStream jsFileStream = getClass().getClassLoader().getResourceAsStream(jsFile);
        return new Scanner(jsFileStream, "UTF-8").useDelimiter("\\A").next();


    }

    /**
     * Retrieve and store the files from the API
     *
     * @param accesToken the access token to use for connection
     * @throws IOException                     Problem when reading or writing a file
     * @throws MultipleMainCategoriesException If one of the files has multiple Main category
     */
    private void retrieveAllFile(String accesToken) throws IOException, MultipleMainCategoriesException {
        if (isExportLaunched()) {
            return;
        }

        setExportLaunched(true);

        //Rest template configuration
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accesToken);
        HttpEntity entity = new HttpEntity(headers);

        //We retrieve the information about the first account of the user
        final ResponseEntity<User> exchange =
                restTemplate.exchange(usersAccountUri.build().toUri(),
                        HttpMethod.GET, entity, User.class);

        final User body = exchange.getBody();
        String id = body.getAccounts()[0].getId();

        //We retrieve the documents metadata
        final ResponseEntity<Documents> documentsResponse = restTemplate.exchange(documentAccountUri.buildAndExpand(id).toUri(), HttpMethod.GET, entity, Documents.class);

        final Document[] documents = documentsResponse.getBody().getDocuments();

        for (Document document : documents) {


            try {
                String fileName = String.format("%tF_%s_%s%s.pdf",
                        document.getIssued(),
                        document.getVendor().replaceAll(" ", ""),
                        document.getTotalInPreferredCurrency().toString().replace('.', ','),
                        extractNotesInfo(document.getNotes()));

                String subDir;
                String mainCategory = extractMainCategory(document.getCategories());
                if (mainCategory != null) {
                    subDir = String.format("postDate_%tF/%s/%s", new Date(), mainCategory, extractFirstDestinationCategory(document.getCategories()));
                } else {
                    subDir = String.format("postDate_%tF/%s", new Date(), extractFirstDestinationCategory(document.getCategories()));
                }

                for (String destinationDir : destinationDirs) {
                    File subDirTotal = new File(destinationDir, subDir);
                    if (!subDirTotal.exists()) {
                        subDirTotal.mkdirs();
                    }

                    File destinationFile = createDestinationFile(fileName, subDirTotal, null);


                    try (final InputStream in = document.getAttachment().getUrl().openStream();
                         final FileOutputStream out = new FileOutputStream(destinationFile)) {
                        System.out.println("retrieving file " + destinationFile);
                        FileCopyUtils.copy(in, out);
                    }
                    manageLog(destinationFile);
                }

            } catch (MultipleMainCategoriesException e) {
                throw new MultipleMainCategoriesException("document " + document + " has Multiple Main Categories. See root cause for detail", e);
            } catch (IOException e) {
                System.err.println("Error when trying to recover document " + document);
                throw e;
            }

            Collections.sort(fileList);
            System.out.println("Final ordered list : ");
            for (String s : fileList) {
                System.out.println(s);
            }
        }
    }

    private void manageLog(File destinationFile) {
        String startingLogEntry = destinationFile.toPath().toString();
        for (String destinationDir : destinationDirs) {
            if (startingLogEntry.startsWith(destinationDir)) {
                fileList.add(startingLogEntry.substring(destinationDir.length()));
                return;
            }
        }
        fileList.add(startingLogEntry);
    }


    /**
     * Create of an indexed fileName
     *
     * @param fileName    file Name
     * @param subDirTotal final SubDir
     * @param index       current Index
     * @return the indexed filename
     * @throws IOException if a I/O is raised.
     */
    private File createDestinationFile(String fileName, File subDirTotal, Integer index) throws IOException {
        String indexedFileName = fileName;
        int nextIndex;
        if (index != null) {
            if (index > maxIndexation) {
                throw new FileNotFoundException(fileName + " plus indexation");
            }
            int extensionsIndex = fileName.lastIndexOf(".");
            String extension = fileName.substring(extensionsIndex);

            indexedFileName = fileName.substring(0, extensionsIndex) + "_(" + index + ")" + extension;
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

    /**
     * Extract the main category from the provided categories
     *
     * @param categories the categories
     * @return <code>null</code> if default (no main cateoory), name of the main category
     * @throws MultipleMainCategoriesException if multiple main category were found
     */
    private String extractMainCategory(String[] categories) throws MultipleMainCategoriesException {
        String mainCategoryToReturn = null;
        for (String category : categories) {
            for (String mainCategory : mainCategories) {
                if (mainCategory.equalsIgnoreCase(category)) {
                    if (mainCategoryToReturn != null) {
                        throw new MultipleMainCategoriesException(mainCategory + "," + mainCategoryToReturn);
                    }
                    mainCategoryToReturn = mainCategory;
                }
            }
        }
        return mainCategoryToReturn;
    }


    /**
     * Extract eh destination category. Will return the first, non-main, category
     *
     * @param categories the categories to parse
     * @return the firste destination category
     */
    private String extractFirstDestinationCategory(String[] categories) {
        for (String category : categories) {
            if (!category.equals(this.toSentCategory)) {
                if (mainCategoriesContains(category)) {
                    continue;
                }
                return category;
            }
        }
        return noCategoryDir;
    }

    private boolean mainCategoriesContains(String category) {
        for (String mainCategory : mainCategories) {
            if (mainCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse note info, extract information under type as type=<value>
     *
     * @param notes the notes to parse
     * @return the value associated to key type, or null
     */
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