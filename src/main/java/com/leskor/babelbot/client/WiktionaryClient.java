package com.leskor.babelbot.client;

import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface WiktionaryClient {

    @GetExchange("/w/api.php?action=query&titles={term}&format=json&prop=extracts&redirects=1")
    DefinitionResponse getDefinition(@PathVariable String term);

    public record DefinitionResponse(String batchcomplete, Query query) {
        public record Query(Map<String, Page> pages) {
            public record Page(int pageid, int ns, String title, String extract) {
            }
        }
    }
}
