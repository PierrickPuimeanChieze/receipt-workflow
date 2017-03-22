package be.cleitech.receipt.tasks;

import be.cleitech.receipt.MailManager;
import be.cleitech.receipt.dropbox.DropboxService;
import be.cleitech.receipt.shoeboxed.MultipleMainCategoriesException;
import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import be.cleitech.receipt.shoeboxed.domain.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.mail.MessagingException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@Component
public class PublishTask {


    private static Log LOG = LogFactory.getLog(PublishTask.class);

    private DropboxService dropboxService;

    @Value("${shoeboxed.toSentCategory}")
    private String toSentCategory;

    @Value("${destinationDirs}")
    private String destinationDir;

    private String noCategoryDir = "noCategoryDir";
    private static final String PROPERTY_NAME_TYPE = "type";
    private String[] specialCategoryMarkers = new String[]{PROPERTY_NAME_TYPE};

    /**
     * The "main" categories
     **/
    private String[] mainCategories = new String[]{"Diving"};
    private final int maxIndexation = 150;


    private ShoeboxedService shoeboxedService;

    private MailManager mailManager;
    private ApplicationContext applicationContext;
    @Autowired
    public PublishTask(DropboxService dropboxService, ShoeboxedService shoeboxedService, MailManager mailManager, ApplicationContext applicationContext) {
        this.dropboxService = dropboxService;
        this.shoeboxedService = shoeboxedService;
        this.mailManager = mailManager;
        this.applicationContext = applicationContext;
    }


    /**
     * Retrieve and store the files from the API
     *
     * @throws IOException                     Problem when reading or writing a file
     * @throws MultipleMainCategoriesException If one of the files has multiple Main category
     */
    public PublishTaskResult retrieveAllFile() {

        Resource resource = applicationContext.getResource("classpath:/templates/uploadResultViewTemplate.html");
        PublishTaskResult result = new PublishTaskResult();

        String todayPostDir = String.format("postDate_%tF", new Date());
        final List<Document> documents = shoeboxedService.retrieveDocument(toSentCategory);

        result.setDocumentsToProceed(documents);
        for (Document document : documents) {
            String fileName = String.format("%tF_%s_%s%s.pdf",
                    document.getIssued(),
                    document.getVendor().replaceAll(" ", ""),
                    document.getTotal().toString().replace('.', ','),
                    "_" + extractTypeInfoFromCategory(document.getCategories()));
            Path categorySubDirPath = Paths.get("");
            try {


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

                File destinationFile = retrieveFileToLocal(document, fileName, subDirTotal);
                String replace = categorySubDirPath.resolve(destinationFile.getName()).toString().replace("\\", "/");
                dropboxService.uploadFile(destinationFile, replace, this);
//                List<String> categories = document.getCategories();
//                categories.remove(toSentCategory);
//                shoeboxedService.updateMetadata(document.getId(), categories);
                result.addSuccessDocument(document, fileName);
            } catch (Exception e) {
                LOG.error("Unable to treat file " + fileName + " for document " + document);
                result.addFailedDocument(document, fileName);
            }

        }
        try {
            mailManager.sentExtractionResults(result.getUploadedFile());
            result.setMailSent(true);
        } catch (MessagingException ex) {
            result.setMailSent(false);
        }
        return result;

    }

    private File retrieveFileToLocal(Document document, String fileName, File subDirTotal) throws IOException {
        File destinationFile = createDestinationFile(fileName, subDirTotal, null);
        //TODO eventually, try to pipe the stream directly to Dropbox, no local copy.
        try (final InputStream in = document.getAttachment().getUrl().openStream();
             final FileOutputStream out = new FileOutputStream(destinationFile)) {
            LOG.info("retrieving file " + destinationFile);
            FileCopyUtils.copy(in, out);
        }
        return destinationFile;
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
    private String extractMainCategory(Iterable<String> categories) throws MultipleMainCategoriesException {
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
    private String extractFirstDestinationCategory(Iterable<String> categories) {
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
     * Parse catgories, searching category starting with <code>{@link #PROPERTY_NAME_TYPE}:</code>
     *
     * @param categories categories to parse
     * @return type info, or empty value
     */
    private String extractTypeInfoFromCategory(List<String> categories) {
        String propertyMarker = PROPERTY_NAME_TYPE + ":";
        for (String category : categories) {
            if (category.startsWith(propertyMarker)) {
                return category.substring(propertyMarker.length());
            }
        }
        return "";
    }



}