package com.cleitech.shoeboxed.commons;

import com.cleitech.shoeboxed.Utils;
import com.cleitech.shoeboxed.domain.Document;
import com.cleitech.shoeboxed.domain.Documents;
import com.cleitech.shoeboxed.domain.User;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Pierrick Puimean-Chieze on 27-12-16.
 */
public class ShoeboxedService {
    private String redirectUrl;
    private static final String RESPONSE_TYPE = "token";
    private static final String SCOPE = "all";
    private String clientId;
    private String accessToken;
//    private static final String PROCESSING_STATE = "PROCESSED";
    private static final String PROCESSING_STATE = "NEEDS_USER_PROCESSING";
//    private static final String PROCESSING_STATE = "NEED_SYSTEM_PROCESSING";
    private RestTemplate restTemplate = new RestTemplate();

    private static final String[] SHOEBOXED_EXPORTER_PROPERTIES_PATHS = new String[]{
            "./shoeboxedExporter.properties",
            "/etc/shoeboxed-toolsuite/shoeboxedExporter.properties",
            System.getenv("APPDATA") + "/shoeboxed-toolsuite/shoeboxedExporter.properties",
            "~/.shoeboxed-toolsuite/shoeboxedExporter.properties"
    };

    private ShoeboxedService(String redirectUrl, String clientId) {
        this.redirectUrl = redirectUrl;
        this.clientId = clientId;
        HttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);
        restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());

    }

    public static ShoeboxedService createFromDefaultConfFilePath() throws IOException {
        Properties properties = new Properties();
        java.io.File shoeboxedPropertiesFile = Utils.findConfFile(SHOEBOXED_EXPORTER_PROPERTIES_PATHS);
        if (shoeboxedPropertiesFile == null) {
            throw new IOException("Unable to find file shoeboxedExporter.properties");
        }
        properties.load(new FileInputStream(shoeboxedPropertiesFile));
        String clientId = properties.getProperty("clientId");
        String redirectUrl = properties.getProperty("redirectUrl");

        return new ShoeboxedService(redirectUrl, clientId);

    }

    public void authorize() {
        this.accessToken = retrieveAccessToken();
    }

    /**
     * Allow to retrieve an acess token
     * @return the access token to retrieve
     */
    private String retrieveAccessToken() {
        final StringBuilder buffer = new StringBuilder();
        final String url = UriComponentsBuilder.fromPath("https://id.shoeboxed.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", RESPONSE_TYPE)
                .queryParam("scope", SCOPE)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("state", "CRT")
                .build().toUriString();


        // You can optionally pass a Settings object here,
        // constructed using Settings.Builder
        JBrowserDriver driver = new JBrowserDriver(Settings.builder().
                timezone(Timezone.AMERICA_NEWYORK).build());

        // This will block for the page load and any
        // associated AJAX requests
        driver.get(url);

        driver.findElementById("username").sendKeys("pierrick.puimean-chieze@cleitech-solutions.be");
        driver.findElementById("password").sendKeys("QzuD0iAPt3yiQCceJItn");
        driver.findElementById("loginForm").submit();

        System.out.println("Authentication done");
        final String previousUrl = driver.getCurrentUrl();
        System.out.println("previousUrl ; " + previousUrl);
        driver.findElementByName("Allow").click();
        System.out.println("Allowing clicked");
        String currentURL = driver.getCurrentUrl();
        System.out.println("URL just after click : " + currentURL);
        while (currentURL.equals(previousUrl)) {
            int currentStatus = driver.getStatusCode();
            if (currentStatus != 200) {
                throw new RuntimeException("Wrong status " + currentStatus);
            }
            currentURL = driver.getCurrentUrl();
        }
        // Returns the page source in its current state, including
        // any DOM updates that occurred after page load
        if (currentURL.startsWith(redirectUrl) && currentURL.contains("access_token") && buffer.length() == 0) {
            try {
                URL url1 = new URL(currentURL);
                String[] params = url1.getRef().split("&");
                Map<String, String> map = new HashMap<>();
                for (String param : params) {
                    String name = param.split("=")[0];
                    String value = param.split("=")[1];
                    map.put(name, value);
                }
                String access_token = map.get("access_token");
                buffer.append(access_token);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        // Close the browser. Allows this thread to terminate.
        driver.quit();

        return buffer.toString();
    }

    /**
     * Allow to upload a document
     * @param tempFileName the path to thedocument to upload
     * @return the status of the upload
     */
    public HttpStatus uploadDocument(Path tempFileName) {

        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com/v2/accounts/{accountId}/documents/?");
//        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString("http://localhost:9999/test");
        String url = uriComponentsBuilder.buildAndExpand(retrieveAccountId()).toUriString();

        //TODO LOG this shit
        System.out.println("upload " + tempFileName + " to shoeboxed");

        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accessToken);

        final java.io.File file = tempFileName.toFile();
        Resource resourceToUpload = new FileSystemResource(file);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("attachment", resourceToUpload);
        body.add("document", "{ \"processingState\": \"" +
                PROCESSING_STATE +
                "\", \"type\":\"receipt\"}");
        HttpEntity entity = new HttpEntity<>(body, headers);
        final ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(url, entity, String.class);
        final HttpStatus statusCode = stringResponseEntity.getStatusCode();
        System.out.println(stringResponseEntity.getBody());
        return statusCode;
    }

    /**
     * Allow to retrieve the first account Id
     * @return the first account Id
     */
    private String retrieveAccountId() {
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accessToken);

        UriComponentsBuilder usersAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/user/");
        HttpEntity entity = new HttpEntity(headers);

        final ResponseEntity<User> exchange =
                restTemplate.exchange(usersAccountUri.build().toUri(),
                        HttpMethod.GET, entity, User.class);

        final User body = exchange.getBody();
        return body.getAccounts()[0].getId();

    }

    /**
     * Allow to retrieve document
     * @param categoryFilter the category of the searched document
     * @return the list of Document
     */
    public Document[] retrieveDocument(String categoryFilter) {

        UriComponentsBuilder getDocumentsAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/accounts/{accountId}/documents/")
                .queryParam("limit", 100)
                .queryParam("type", "receipt")
                .queryParam("category", categoryFilter)
                .queryParam("trashed", false);

        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity entity = new HttpEntity(headers);

        //We retrieve the documents metadata
        final URI url = getDocumentsAccountUri.buildAndExpand(retrieveAccountId()).toUri();
        final ResponseEntity<Documents> documentsResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Documents.class);

        return documentsResponse.getBody().getDocuments();

    }
}
