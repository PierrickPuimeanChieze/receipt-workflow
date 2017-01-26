package be.cleitech.shoeboxed.extractor;

import be.cleitech.shoeboxed.Utils;
import be.cleitech.shoeboxed.ShoeboxedService;
import be.cleitech.shoeboxed.domain.Document;
import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;

import javax.mail.MessagingException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class ExtractorMain{


    private String dropboxPassword = "teocali@reschaotica.com";
    private final static String[] DROPBOX_SECRET_PATHS = new String[]{
            "./dropbox_client_secret.json",
            "/etc/shoeboxed-toolsuite/dropbox_client_secret.json",
            System.getenv("APPDATA") + "/shoeboxed-toolsuite/dropbox_client_secret.json",
            "~/.shoeboxed-toolsuite/dropbox_client_secret.json"
    };

    private final String DROPBOX_UPLOAD_PATH = "/cfp consulting/TEST";


    private String toSentCategory = "to send";

    @Value("${destinationDirs}")
    private String destinationDir;

    private String noCategoryDir = "noCategoryDir";
    private static final String PROPERTY_NAME_TYPE = "type";
    private String[] specialCategoryMarkers = new String[]{PROPERTY_NAME_TYPE};
    private DbxAuthFinish authFinish;

    /**
     * The "main" categories
     **/
    private String[] mainCategories = new String[]{"Diving"};
    private final int maxIndexation = 150;

    private Set<String> fileList = new TreeSet<>();

    private ShoeboxedService shoeboxedService;
    private DbxAppInfo appInfo;
    private String dropboxAccessToken;
    private MailManager mailManager;

    public ExtractorMain(ShoeboxedService shoeboxedService, MailManager mailManager) {
        this.shoeboxedService = shoeboxedService;
        this.mailManager = mailManager;
    }


    private void initDropboxSDK() throws JsonReader.FileLoadException, IOException {
        File dropboxAccessTokenFile = new File("./dropboxAccessToken");
        if (!dropboxAccessTokenFile.exists()) {
            dropboxAccessToken = retrieveDropBoxAccessToken();
            try (FileWriter fileWriter = new FileWriter(dropboxAccessTokenFile)) {
                fileWriter.write(dropboxAccessToken);
            }

        } else {
            try (
                    FileReader fileReader = new FileReader(dropboxAccessTokenFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader)
            ) {
                dropboxAccessToken = bufferedReader.readLine();
            }
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

    private void findAndInputLogin(JBrowserDriver driver) throws DropboxAuthenticationOption {
        for (WebElement login_email_element : driver.findElementsByName("login_email")) {
            if (login_email_element.getTagName().equalsIgnoreCase("input")) {
                login_email_element.sendKeys(dropboxPassword);
                return;
            }
        }
        throw new DropboxAuthenticationOption("Unable to find a login Input field", driver);
    }


    /**
     * Retrieve and store the files from the API
     *
     * @throws IOException                     Problem when reading or writing a file
     * @throws MultipleMainCategoriesException If one of the files has multiple Main category
     */
    private void retrieveAllFile() throws IOException, MultipleMainCategoriesException, DbxException, MessagingException {

        String todayPostDir = String.format("postDate_%tF", new Date());

        final Document[] documents = shoeboxedService.retrieveDocument(toSentCategory);
        for (Document document : documents) {
            try {
                String fileName = String.format("%tF_%s_%s%s.pdf",
                        document.getIssued(),
                        document.getVendor().replaceAll(" ", ""),
                        document.getTotal().toString().replace('.', ','),
                        "_" + extractTypeInfoFromCategory(document.getCategories()));

                Path categorySubDirPath = Paths.get("");
                String mainCategory = extractMainCategory(document.getCategories());
                if (mainCategory != null) {
                    categorySubDirPath = categorySubDirPath.resolve(mainCategory);
                }

                categorySubDirPath = categorySubDirPath.resolve(extractFirstDestinationCategory(document.getCategories()));

                Path finalSubDirTotalPath = Paths.get(destinationDir).resolve(todayPostDir).resolve(categorySubDirPath);
                File subDirTotal = finalSubDirTotalPath.toFile();
                if (!subDirTotal.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    subDirTotal.mkdirs();
                }

                File destinationFile = createDestinationFile(fileName, subDirTotal, null);
                copyDocumentToLocal(document, destinationFile);
                String replace = categorySubDirPath.resolve(destinationFile.getName()).toString().replace("\\", "/");
                uploadFile(destinationFile, replace);
                manageLog(replace);

            } catch (MultipleMainCategoriesException e) {
                throw new MultipleMainCategoriesException("document " + document + " has Multiple Main Categories. See root cause for detail", e);
            } catch (IOException e) {
                System.err.println("Error when trying to recover document " + document);
                throw e;
            }


        }
        mailManager.sentExtractionResults(fileList);
    }

    private void copyDocumentToLocal(Document document, File destinationFile) throws IOException {
        //TODO eventually, try to pipe the stream directly to Dropbox, no local copy.
        try (final InputStream in = document.getAttachment().getUrl().openStream();
             final FileOutputStream out = new FileOutputStream(destinationFile)) {
            System.out.println("retrieving file " + destinationFile);
            FileCopyUtils.copy(in, out);
        }
    }

    private void manageLog(String destinationFile) {

        fileList.add(destinationFile);
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
                if (isSpecialCategory(category)) {
                    continue;
                }
                return category;
            }
        }
        return noCategoryDir;
    }

    private boolean isSpecialCategory(String category) {
        for (String specialCategoryMarker : specialCategoryMarkers) {
            if (category.startsWith(specialCategoryMarker + ":")) {
                return true;
            }
        }
        return false;
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
    private String extractTypeInfoFromNotes(String notes) {
        if (notes == null) {
            return "";
        }
        Properties notesP = new Properties();
        try {
            notesP.load(new StringReader(notes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String receiptType = notesP.getProperty(PROPERTY_NAME_TYPE);
        if (receiptType == null) {
            return "";
        }
        return "_" + receiptType;
    }

    /**
     * Parse catgories, searching category starting with <code>{@link #PROPERTY_NAME_TYPE}:</code>
     *
     * @param categories categories to parse
     * @return type info, or empty value
     */
    private String extractTypeInfoFromCategory(String[] categories) {
        String propertyMarker = PROPERTY_NAME_TYPE + ":";
        for (String category : categories) {
            if (category.startsWith(propertyMarker)) {
                return category.substring(propertyMarker.length());
            }
        }
        return "";
    }

    public void run(String[] args) throws Exception {

        initDropboxSDK();
        retrieveAllFile();
    }

    private void uploadFile(File fileToUpload, String fileName) throws DbxException, IOException {
        // Create Dropbox client
        DbxRequestConfig config = new DbxRequestConfig("shoeboxed-toolsuite");
        DbxClientV2 client = new DbxClientV2(config, dropboxAccessToken);

        // Upload file to Dropbox
        try (InputStream in = new FileInputStream(fileToUpload)) {
            client.files().uploadBuilder(DROPBOX_UPLOAD_PATH + "/" + fileName)
                    .uploadAndFinish(in);
        }
    }


}