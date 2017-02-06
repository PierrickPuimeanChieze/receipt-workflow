package be.cleitech.receipt.shoeboxed;

/**
 * Created by pierrick on 03.02.17.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @Author jfield@pivotal.io
 */

// Cf:  http://stackoverflow.com/questions/23264044/spring-data-rest-disable-hypertext-application-language-hal-in-json-applica
// Cf:  http://stackoverflow.com/questions/23239052/why-does-resttemplate-not-bind-response-representation-to-pagedresources

@Configuration
public class ShoeboxedRestResource {


    @Bean
    public MappingJackson2HttpMessageConverter ConfigConverter(ObjectMapper mapper) {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/hal+json"));
        converter.setObjectMapper(mapper);

        return converter;

    }

    @Bean
    public List<HttpMessageConverter<?>> ConfigConverterList (MappingJackson2HttpMessageConverter aConverter) {

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(aConverter);
        return converters;

    }

}
