package com.makerspacetools.config;

import com.makerspacetools.auth.MakerSpaceAuthClient;
import com.makerspacetools.auth.MakerSpaceAuthService;
import com.makerspacetools.client.MakerSpaceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

/**
 * Configuration for MakerSpace API client.
 */
@Configuration
class MakerSpaceClientConfig {

    private final MakerSpaceClientProperties properties;

    @Autowired
    MakerSpaceClientConfig(MakerSpaceClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a MakerSpace client proxy.
     *
     * @param makerSpaceRestClient configured RestClient
     * @return MakerSpaceClient proxy
     */
    @Bean
    MakerSpaceClient makerSpaceClient(RestClient makerSpaceRestClient) {
        return createClient(makerSpaceRestClient, MakerSpaceClient.class);
    }

    /**
     * Creates a MakerSpace authentication client proxy.
     *
     * @param makerSpaceAuthRestClient configured RestClient
     * @return MakerSpaceAuthClient proxy
     */
    @Bean
    MakerSpaceAuthClient makerSpaceAuthClient(RestClient makerSpaceAuthRestClient) {
        return createClient(makerSpaceAuthRestClient, MakerSpaceAuthClient.class);
    }

    /**
     * Creates a RestClient with authorization and JSON defaults.
     *
     * @param authService authentication service
     * @return RestClient for MakerSpace API
     */
    @Bean
    RestClient makerSpaceRestClient(MakerSpaceAuthService authService) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(authService.getAccessToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Creates a RestClient for authentication calls.
     *
     * @return RestClient for auth endpoints
     */
    @Bean
    RestClient makerSpaceAuthRestClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> headers.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }

    private <T> T createClient(RestClient restClient, Class<T> clientType) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientType);
    }
}
