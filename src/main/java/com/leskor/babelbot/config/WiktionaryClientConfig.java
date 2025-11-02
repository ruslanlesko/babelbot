package com.leskor.babelbot.config;

import com.leskor.babelbot.client.WiktionaryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WiktionaryClientConfig {

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(RestClient.Builder restClientBuilder,
            @Value("${babelbot.wiktionary.base-url}") String baseUrl) {
        RestClient restClient = restClientBuilder.baseUrl(baseUrl)
                .defaultHeader("User-Agent",
                        "BabelBot/0.1 (https://github.com/ruslanlesko/babelbot; ruslanlesko@gmail.com)")
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    }

    @Bean
    WiktionaryClient wiktionaryClient(HttpServiceProxyFactory httpServiceProxyFactory) {
        return httpServiceProxyFactory.createClient(WiktionaryClient.class);
    }
}
