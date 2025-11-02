package com.leskor.babelbot.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leskor.babelbot.model.Definition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WiktionaryDefinitionParserTest {

    private WiktionaryDefinitionParser parser;

    @BeforeEach
    void setUp() {
        parser = new WiktionaryDefinitionParser();
    }

    @Test
    void testParseCat() throws IOException {
        String htmlContent = loadHtmlResource("wiktionary/cat.html");

        Optional<Definition> result = parser.parse("cat", htmlContent);

        assertTrue(result.isPresent());
        assertEquals("Noun", result.get().partOfSpeech());
        assertIterableEquals(List.of(
                "A carnivorous, four-legged, generally furry domesticated species (Felis catus) of feline animal, commonly kept as a house pet. [from 8th c.]",
                "Any similar, chiefly non-domesticated, animal of the family Felidae, which includes bobcats, caracals, cheetahs, cougars, leopards, lions, lynxes, tigers, and other such species."),
                result.get().meanings());
        assertEquals(
                "From Middle English cat, catte, from Old English catt (“male cat”), catte (“female cat”), from Proto-West Germanic *kattu, from Proto-Germanic *kattuz, generally thought to be from Late Latin cattus (“domestic cat”) (c. 350, Palladius), from Latin catta (c. 75 A.D., Martial), from an Afroasiatic language.",
                result.get().etymology());
        assertIterableEquals(List.of("grimalkin", "kitty", "kitty-cat", "puss", "pussy", "pussy-cat"),
                result.get().synonyms());
    }

    @Test
    void testParseChleb() throws IOException {
        String htmlContent = loadHtmlResource("wiktionary/chleb.html");

        Optional<Definition> result = parser.parse("chleb", htmlContent);

        assertTrue(result.isPresent());
        assertEquals("Noun", result.get().partOfSpeech());
        assertIterableEquals(List.of(
                "bread; sacramental bread (baked good made of flour and water, usually sourdough or yeast; single loaf of this, sometimes used in the Christian ritual of the Eucharist before the consecration)",
                "bread (maintenance, food, often paired with salt)"), result.get().meanings());
        assertEquals("Inherited from Proto-Slavic *xlě̀bъ. First attested in the 14th century.",
                result.get().etymology());
        assertIterableEquals(List.of(), result.get().synonyms());
    }

    @Test
    void testParseDrive() throws IOException {
        String htmlContent = loadHtmlResource("wiktionary/drive.html");

        Optional<Definition> result = parser.parse("drive", htmlContent);

        assertTrue(result.isPresent());
        assertEquals("Verb", result.get().partOfSpeech());
        assertIterableEquals(List.of("To operate (a wheeled motorized vehicle).",
                "To travel by operating a wheeled motorized vehicle.",
                "To convey (a person, etc.) in a wheeled motorized vehicle.", "To operate (an aircraft); to pilot.",
                "To direct a vehicle powered by a horse, ox or similar animal."), result.get().meanings());
        assertEquals(
                "From Middle English driven, from Old English drīfan (“to drive, force, move”), from Proto-West Germanic *drīban, from Proto-Germanic *drībaną (“to drive”), from Proto-Indo-European *dʰreybʰ- (“to drive, push”).",
                result.get().etymology());
        assertIterableEquals(List.of("ride"), result.get().synonyms());
    }

    @Test
    void testParseStrefa() throws IOException {
        String htmlContent = loadHtmlResource("wiktionary/strefa.html");

        Optional<Definition> result = parser.parse("strefa", htmlContent);

        assertTrue(result.isPresent());
        assertEquals("Noun", result.get().partOfSpeech());
        assertIterableEquals(List.of("zone", "area", "region", "belt"), result.get().meanings());
        assertEquals("Borrowed from German Streifen.", result.get().etymology());
        assertIterableEquals(List.of("zona"), result.get().synonyms());
    }

    private String loadHtmlResource(String resourcePath) throws IOException {
        String path = getClass().getClassLoader().getResource(resourcePath).getPath();
        return Files.readString(Paths.get(path));
    }
}
