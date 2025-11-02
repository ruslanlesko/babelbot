package com.leskor.babelbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leskor.babelbot.client.WiktionaryClient;
import com.leskor.babelbot.model.Definition;
import com.leskor.babelbot.parser.WiktionaryDefinitionParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefinitionServiceTest {
    private static final Definition DEFINITION = new Definition("test", "noun",
            List.of("a procedure intended to establish the quality, performance, or reliability of something"),
            "from Latin testum", List.of("exam", "trial"));

    @Mock
    private WiktionaryClient wiktionaryClient;

    @Mock
    private WiktionaryDefinitionParser definitionParser;

    private DefinitionService definitionService;

    @BeforeEach
    void setUp() {
        definitionService = new DefinitionService(wiktionaryClient, definitionParser);
    }

    @Test
    void getDefinition_shouldReturnDefinition_whenTermExists() {
        String term = "test";
        String extract = "test definition\\nwith newline\\\"quote";
        String sanitizedExtract = "test definition\nwith newline\"quote";

        var definitionResponse = new WiktionaryClient.DefinitionResponse("true",
                new WiktionaryClient.DefinitionResponse.Query(
                        Map.of("1", new WiktionaryClient.DefinitionResponse.Query.Page(1, 0, "test", extract))));

        when(wiktionaryClient.getDefinition(term)).thenReturn(definitionResponse);
        when(definitionParser.parse(term, sanitizedExtract)).thenReturn(Optional.of(DEFINITION));

        Optional<Definition> result = definitionService.getDefinition(term);

        assertTrue(result.isPresent());
        assertEquals(DEFINITION, result.get());
        verify(wiktionaryClient).getDefinition(term);
        verify(definitionParser).parse(term, sanitizedExtract);
    }

    @Test
    void getDefinition_shouldReturnEmpty_whenParserReturnsEmpty() {
        String term = "test";
        String extract = "test definition";

        var definitionResponse = new WiktionaryClient.DefinitionResponse("true",
                new WiktionaryClient.DefinitionResponse.Query(
                        Map.of("1", new WiktionaryClient.DefinitionResponse.Query.Page(1, 0, "test", extract))));

        when(wiktionaryClient.getDefinition(term)).thenReturn(definitionResponse);
        when(definitionParser.parse(eq(term), any())).thenReturn(Optional.empty());

        Optional<Definition> result = definitionService.getDefinition(term);

        assertFalse(result.isPresent());
        verify(wiktionaryClient).getDefinition(term);
        verify(definitionParser).parse(term, extract);
    }

    @Test
    void getDefinition_shouldReturnEmpty_whenNoPagesFound() {
        String term = "test";

        var definitionResponse = new WiktionaryClient.DefinitionResponse("true",
                new WiktionaryClient.DefinitionResponse.Query(Map.of()));

        when(wiktionaryClient.getDefinition(term)).thenReturn(definitionResponse);

        Optional<Definition> result = definitionService.getDefinition(term);

        assertFalse(result.isPresent());
        verify(wiktionaryClient).getDefinition(term);
        verify(definitionParser, never()).parse(any(), any());
    }

    @Test
    void getDefinition_shouldSanitizeHtmlCorrectly() {
        String term = "test";
        String extract = "definition\\nwith\\nnewlines\\\"and\\\"quotes";
        String expectedSanitized = "definition\nwith\nnewlines\"and\"quotes";

        var definitionResponse = new WiktionaryClient.DefinitionResponse("true",
                new WiktionaryClient.DefinitionResponse.Query(
                        Map.of("1", new WiktionaryClient.DefinitionResponse.Query.Page(1, 0, "test", extract))));

        when(wiktionaryClient.getDefinition(term)).thenReturn(definitionResponse);
        when(definitionParser.parse(term, expectedSanitized)).thenReturn(Optional.of(DEFINITION));

        definitionService.getDefinition(term);

        verify(definitionParser).parse(term, expectedSanitized);
    }
}
