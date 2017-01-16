package be.cleitech.shoeboxed.extractor;

import com.cleitech.shoeboxed.domain.Document;
import com.cleitech.shoeboxed.commons.ShoeboxedService;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.*;

public class ExtractorMain {



    private String toSentCategory = "to send";
    private String[] destinationDirs = {"C:\\Users\\Pierrick\\Google Drive\\Cleitech\\Facturettes\\Test\\",};
    private String noCategoryDir = "noCategoryDir";

    /**
     * The "main" categories
     **/
    private String[] mainCategories = new String[]{"Diving"};
    private final int maxIndexation = 150;

    private ArrayList<String> fileList = new ArrayList<>();

    private ShoeboxedService shoeboxedService;
    public void init() throws Exception {

        Properties properties = new Properties();
        properties.load(new FileInputStream("shoeboxedExporter.properties"));
        String clientId = properties.getProperty("clientId");
        destinationDirs = properties.getProperty("destinationDirs").split(";");
        noCategoryDir = properties.getProperty("noCategoryDir");
        String redirectUrl = properties.getProperty("redirectUrl");

        shoeboxedService = new ShoeboxedService(redirectUrl, clientId);
        shoeboxedService.authorize();
    }


    /**
     * Retrieve and store the files from the API
     *
     * @throws IOException                     Problem when reading or writing a file
     * @throws MultipleMainCategoriesException If one of the files has multiple Main category
     */
    private void retrieveAllFile() throws IOException, MultipleMainCategoriesException {

        final Document[] documents = shoeboxedService.retrieveDocumentToSend(toSentCategory);
        for (Document document : documents) {


            try {
                String fileName = String.format("%tF_%s_%s%s.pdf",
                        document.getIssued(),
                        document.getVendor().replaceAll(" ", ""),
                        document.getTotal().toString().replace('.', ','),
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
        final ExtractorMain extractorMain = new ExtractorMain();
        extractorMain.init();
        extractorMain.retrieveAllFile();
    }
}