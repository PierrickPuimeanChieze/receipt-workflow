package be.cleitech.receipt;

import be.cleitech.receipt.shoeboxed.ShoeboxedService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by pierrick on 07.02.17.
 */
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    private static Log LOG = LogFactory.getLog(LoggingRequestInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        ClientHttpResponse response = execution.execute(request, body);

        log(request, body, response);

        return response;
    }

    private void log(HttpRequest request, byte[] body, ClientHttpResponse response) throws IOException {
        LOG.debug("===========================request begin================================================");
        LOG.debug("URI         : " + request.getURI());
        LOG.debug("Method      : " + request.getMethod());
        LOG.debug("Headers     : " + request.getHeaders());
        LOG.debug("Request body: " + new String(body, "UTF-8"));
        LOG.debug("==========================request end================================================");
    }
}