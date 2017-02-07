package be.cleitech.receipt.shoeboxed;

import be.cleitech.receipt.LoggingRequestInterceptor;
import be.cleitech.receipt.shoeboxed.domain.*;
import com.dropbox.core.json.JsonReader;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pierrick Puimean-Chieze on 27-12-16.
 */
public class ShoeboxedService implements AuthenticatedService {
    private static Log LOG = LogFactory.getLog(ShoeboxedService.class);
    private final String password;
    private final static String TOKEN_URL = "https://id.shoeboxed.com/oauth/token";
    private String redirectUrl;
    private static final String RESPONSE_TYPE = "code";
    private static final String SCOPE = "all";
    private String clientId;
    private String clientSecret;

    public ShoeboxedTokenInfo getAccessTokenInfo() {
        return accessTokenInfo;
    }

    private ShoeboxedTokenInfo accessTokenInfo;
    private RestTemplate restTemplate = new RestTemplate();
    private ProcessingState processingState;

    private File accessTokenFile;

    private Gson gson = new Gson();
    private String username;

    public ShoeboxedService(String redirectUrl, String clientId, String clientSecret, ProcessingState processingStateForUpload, File accessTokenFile, String username, String password) {
        this.redirectUrl = redirectUrl;
        this.clientId = clientId;
        this.processingState = processingStateForUpload;
        this.accessTokenFile = accessTokenFile;
        this.password = password;
        this.username = username;
        this.clientSecret = clientSecret;
        HttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);
        restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingRequestInterceptor());
        restTemplate.setInterceptors(interceptors);

    }

    public void initAccessToken() throws JsonReader.FileLoadException, IOException {

        if (!accessTokenFile.exists()) {
            accessTokenInfo = retrieveAccessToken();
            try (FileWriter fileWriter = new FileWriter(accessTokenFile)) {

                gson.toJson(accessTokenInfo, fileWriter);
            }

        } else {
            try (
                    FileReader fileReader = new FileReader(accessTokenFile);
            ) {
                com.google.gson.stream.JsonReader jsonReader = gson.newJsonReader(fileReader);
                accessTokenInfo = gson.fromJson(fileReader, ShoeboxedTokenInfo.class);
            }
        }
    }

    /**
     * Allow to retrieve an acess token
     *
     * @return the access token to retrieve
     */
    private ShoeboxedTokenInfo retrieveAccessToken() throws IOException {
        final String oauthUrl = UriComponentsBuilder.fromUriString("http://id.shoeboxed.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", RESPONSE_TYPE)
                .queryParam("scope", SCOPE)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("state", "CRT")
                .build().toUriString();


        // This will block for the page load and any
        // associated AJAX requests
        System.out.println("go to URL " + oauthUrl);
        System.out.println("\n");
        String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (code == null) {
            throw new RuntimeException("code==null");
        }

        final Instant lastRefresh = Instant.now();

        final String tokenUrl = UriComponentsBuilder.fromUriString(TOKEN_URL)
                .queryParam("grant_type", "authorization_code")
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUrl)
                .build().toUriString();


        HttpHeaders headers = buildHeadersFromClientInfo();


        System.out.println("trying to acess :" + tokenUrl);
        try {
            ResponseEntity<ShoeboxedTokenInfo> exchange = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity(headers), ShoeboxedTokenInfo.class);

            ShoeboxedTokenInfo shoeboxedTokenInfo = exchange.getBody();
            shoeboxedTokenInfo.setLastRefresh(lastRefresh);
            return shoeboxedTokenInfo;
        } catch (HttpClientErrorException ex) {
            System.out.println(ex.getResponseBodyAsString());
            throw ex;
        }
    }

    private HttpHeaders buildHeadersFromClientInfo() {
        HttpHeaders headers = new HttpHeaders();


        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.encodeBase64String(
                auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + encodedAuth;
        headers.set("Authorization", authHeader);
        return headers;
    }

    /**
     * Allow to upload a document
     *
     * @param tempFileName the path to thedocument to upload
     * @return the status of the upload
     */
    public HttpStatus uploadDocument(Path tempFileName) {

        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com/v2/accounts/{accountId}/documents/?");
//        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString("http://localhost:9999/test");
        String url = uriComponentsBuilder.buildAndExpand(retrieveAccountId()).toUriString();


        final java.io.File file = tempFileName.toFile();
        Resource resourceToUpload = new FileSystemResource(file);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("attachment", resourceToUpload);
        body.add("document", "{ \"processingState\": \"" +
                processingState +
                "\", \"type\":\"receipt\"}");
        HttpEntity entity = new HttpEntity<>(body, buildHeadersFromAccessToken());
        final ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(url, entity, String.class);

        return stringResponseEntity.getStatusCode();
    }

    private HttpHeaders buildHeadersFromAccessToken() {
        refreshTokenIfNeeded();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + accessTokenInfo.getAccessToken());
        return headers;
    }

    private void refreshTokenIfNeeded() {
        Instant now = Instant.now();

        Instant accessTokenLastRefresh = accessTokenInfo.getLastRefresh();

        long secondsSinceLastRefresh = Duration.between(accessTokenLastRefresh, now).getSeconds();

        float securityMargin = (float) 0.9;
        if (secondsSinceLastRefresh > (accessTokenInfo.getExpiresIn() * securityMargin)) {
            final String tokenUrl = UriComponentsBuilder.fromUriString(TOKEN_URL)
                    .queryParam("grant_type", "refresh_token")
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", redirectUrl)
                    .queryParam("refresh_token", accessTokenInfo.getRefreshToken())
                    .build().toUriString();

            ResponseEntity<ShoeboxedTokenInfo> refreshResponse = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(buildHeadersFromClientInfo()), ShoeboxedTokenInfo.class);
            if (refreshResponse.getStatusCode() == HttpStatus.OK) {
                accessTokenInfo.setLastRefresh(now);
                ShoeboxedTokenInfo partialTokenInfo = refreshResponse.getBody();
                accessTokenInfo.setAccessToken(partialTokenInfo.getAccessToken());
                accessTokenInfo.setExpiresIn(partialTokenInfo.getExpiresIn());
            }

        }
    }

    /**
     * Allow to retrieve the first account Id
     *
     * @return the first account Id
     */
    private String retrieveAccountId() {

        UriComponentsBuilder usersAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/user/");
        HttpEntity entity = new HttpEntity(buildHeadersFromAccessToken());

        final ResponseEntity<User> exchange =
                restTemplate.exchange(usersAccountUri.build().toUri(),
                        HttpMethod.GET, entity, User.class);

        final User body = exchange.getBody();
        return body.getAccounts()[0].getId();

    }

    /**
     * Allow to retrieve document
     *
     * @param categoryFilter the category of the searched document
     * @return the list of Document
     */
    public Document[] retrieveDocument(String categoryFilter) {

        UriComponentsBuilder getDocumentsAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/accounts/{accountId}/documents/")
                .queryParam("limit", 100)
                .queryParam("type", "receipt")
                .queryParam("category", categoryFilter)
                .queryParam("trashed", false);

        HttpEntity entity = new HttpEntity(buildHeadersFromAccessToken());

        //TODO extract the use of retrieveAccountId
        //We retrieve the documents metadata
        final URI url = getDocumentsAccountUri.buildAndExpand(retrieveAccountId()).toUri();
        final ResponseEntity<Documents> documentsResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Documents.class);

        return documentsResponse.getBody().getDocuments();

    }

    public void updateMetadata(String documentId, List<String> categories) {

        UriComponentsBuilder getDocumentsAccountUri = UriComponentsBuilder.fromUriString("https://api.shoeboxed.com:443/v2/accounts/{accountId}/documents/{documentId}");
        Document newMetadata = new Document();
        newMetadata.setCategories(categories);

        HttpEntity<Document> entity = new HttpEntity<>(newMetadata, buildHeadersFromAccessToken());
        final URI url = getDocumentsAccountUri.buildAndExpand(retrieveAccountId(), documentId).toUri();
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

    }
}
