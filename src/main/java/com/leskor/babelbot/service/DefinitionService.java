package com.leskor.babelbot.service;

import com.leskor.babelbot.client.WiktionaryClient;
import com.leskor.babelbot.model.Definition;
import com.leskor.babelbot.parser.WiktionaryDefinitionParser;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefinitionService {
    private final WiktionaryClient wiktionaryClient;
    private final WiktionaryDefinitionParser definitionParser;

    public DefinitionService(WiktionaryClient wiktionaryClient, WiktionaryDefinitionParser definitionParser) {
        this.wiktionaryClient = wiktionaryClient;
        this.definitionParser = definitionParser;
    }

    public Optional<Definition> getDefinition(String term) {
        WiktionaryClient.DefinitionResponse response = wiktionaryClient.getDefinition(term);
        return response.query()
                .pages()
                .values()
                .stream()
                .findFirst()
                .map(WiktionaryClient.DefinitionResponse.Query.Page::extract)
                .flatMap(extract -> definitionParser.parse(term, sanitizeHtml(extract)));
    }

    private static String sanitizeHtml(String htmlContent) {
        return htmlContent.replace("\\n", "\n").replace("\\\"", "\"");
    }
}
