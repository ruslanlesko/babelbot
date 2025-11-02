package com.leskor.babelbot.parser;

import com.leskor.babelbot.model.Definition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class WiktionaryDefinitionParser {
    private static final String EMPTY_ELEMENT_CLASS = "mw-empty-elt";
    private static final String HEADER_REGEX = "h[2-6]";
    private static final Set<String> VALID_PARTS_OF_SPEECH = Set.of("Noun", "Verb", "Adjective", "Adverb", "Pronoun",
            "Preposition", "Conjunction", "Interjection");

    /**
     * Parses the HTML string obtained from Wiktionary and extracts the definition details.
     * <p>
     * Extracts only first part of the speech if multiple are present.
     * <p>
     * Extracts only first etymology if multiple are present.
     * <p>
     * Extracts multiple meanings for terms related to the first term category.
     * <p>
     * Extracts multiple synonyms for the first term if present.
     *
     * @param term the term being defined
     * @param htmlString raw HTML string from Wiktionary "extract"
     * @return Definition object containing the parsed details, or empty if no substantial definition found
     */
    public Optional<Definition> parse(String term, String htmlString) {
        Document document = Jsoup.parse(htmlString);

        // Find the first substantial language section (skip Translingual and short sections)
        Element languageSection = findFirstSubstantialLanguageSection(document);
        if (languageSection == null) {
            return Optional.empty();
        }

        String partOfSpeech = extractPartOfSpeech(languageSection);
        List<String> meanings = extractMeanings(languageSection, partOfSpeech);
        String etymology = extractEtymology(languageSection);
        List<String> synonyms = extractSynonyms(languageSection, partOfSpeech);

        return Optional.of(new Definition(term, partOfSpeech, meanings, etymology, synonyms));
    }

    private Element findFirstSubstantialLanguageSection(Document document) {
        // Find h2 elements (language sections), skip "Translingual" and short sections
        Elements h2Elements = document.select("h2");
        for (Element h2 : h2Elements) {
            String languageName = h2.text();

            // Skip Translingual section
            if (languageName.equals("Translingual")) {
                continue;
            }

            // Check if this section has substantial content (has etymology or multiple meanings)
            if (hasSubstantialContent(h2)) {
                return h2;
            }
        }
        return null;
    }

    private boolean hasSubstantialContent(Element languageSection) {
        // A section is substantial if it has meaningful content
        // We check the deepest level of the first ordered list to count actual definitions

        Element currentElement = languageSection.nextElementSibling();

        while (currentElement != null && !isLanguageHeader(currentElement)) {
            // Look for the first ordered list (meanings)
            if (currentElement.tagName().equals("ol")) {
                // Navigate to the deepest nested level to count actual meanings
                int meaningCount = countDeepestMeanings(currentElement);
                // Consider substantial if has 2 or more actual meanings
                return meaningCount >= 2;
            }

            currentElement = currentElement.nextElementSibling();
        }

        return false;
    }

    private int countDeepestMeanings(Element ol) {
        Elements topLevelItems = ol.select("> li");
        if (topLevelItems.isEmpty()) {
            return 0;
        }

        // Check if first item has nested ol
        Element firstItem = topLevelItems.first();
        Elements nestedOls = firstItem.select("> ol");

        if (!nestedOls.isEmpty()) {
            // Has nested structure - go deeper
            return countDeepestMeanings(nestedOls.first());
        }
        else {
            // At the deepest level - count non-empty items
            int count = 0;
            for (Element li : topLevelItems) {
                if (!li.hasClass(EMPTY_ELEMENT_CLASS)) {
                    count++;
                }
            }
            return count;
        }
    }

    private String extractPartOfSpeech(Element languageSection) {
        // Find the first h3 or h4 element after language section that represents a part of speech
        Element currentElement = languageSection.nextElementSibling();
        while (currentElement != null && !isLanguageHeader(currentElement)) {
            if (currentElement.tagName().equals("h3") || currentElement.tagName().equals("h4")) {
                String text = currentElement.text();
                if (VALID_PARTS_OF_SPEECH.contains(text)) {
                    return text;
                }
            }
            currentElement = currentElement.nextElementSibling();
        }
        return "";
    }

    private boolean isLanguageHeader(Element element) {
        // Language sections are h2 elements
        return element.tagName().equals("h2");
    }

    private List<String> extractMeanings(Element languageSection, String partOfSpeech) {
        List<String> meanings = new ArrayList<>();

        Element posHeader = findPartOfSpeechHeader(languageSection, partOfSpeech);
        if (posHeader == null) {
            return meanings;
        }

        Element meaningsList = findFirstOrderedListAfterHeader(posHeader);
        if (meaningsList != null) {
            extractMeaningsFromList(meaningsList, meanings);
        }

        return meanings;
    }

    private Element findPartOfSpeechHeader(Element languageSection, String partOfSpeech) {
        Element currentElement = languageSection.nextElementSibling();
        while (currentElement != null && !isLanguageHeader(currentElement)) {
            if ((currentElement.tagName().equals("h3") || currentElement.tagName().equals("h4"))
                    && currentElement.text().equals(partOfSpeech)) {
                return currentElement;
            }
            currentElement = currentElement.nextElementSibling();
        }
        return null;
    }

    private Element findFirstOrderedListAfterHeader(Element header) {
        Element currentElement = header.nextElementSibling();
        while (currentElement != null && !currentElement.tagName().matches(HEADER_REGEX)) {
            if (currentElement.tagName().equals("ol")) {
                return currentElement;
            }
            currentElement = currentElement.nextElementSibling();
        }
        return null;
    }

    private void extractMeaningsFromList(Element meaningsList, List<String> meanings) {
        // Check if this ol contains nested ols - we want to extract from the deepest level
        Elements topLevelItems = meaningsList.select("> li");
        if (topLevelItems.isEmpty()) {
            return;
        }

        // Check first item for nested structure
        Element firstItem = topLevelItems.first();
        Elements nestedOls = firstItem.select("> ol");

        if (!nestedOls.isEmpty()) {
            // Has nested structure - recursively go deeper
            extractMeaningsFromList(nestedOls.first(), meanings);
        }
        else {
            // No nested structure - extract meanings from this level
            for (Element li : topLevelItems) {
                String meaning = extractMeaningText(li);
                if (!meaning.isEmpty()) {
                    meanings.add(meaning);
                }
            }
        }
    }

    private String extractMeaningText(Element li) {
        // Skip elements with class "mw-empty-elt"
        if (li.hasClass(EMPTY_ELEMENT_CLASS)) {
            return "";
        }

        // Clone the element to avoid modifying the original
        Element clone = li.clone();

        // Remove nested ol and ul elements (sub-meanings, examples, etc.)
        clone.select("ol, ul, dl").remove();

        // Remove specific span elements that contain metadata
        clone.select("span.senseid").remove();
        clone.select("span[id]").remove();

        // Get the text and clean it up
        String text = clone.text();

        // Remove leading markers like numbers or letters
        text = text.replaceAll("^\\d+\\.?\\s*", "");
        text = text.replaceAll("^[a-z]\\)\\s*", "");

        // Remove parenthetical context markers at the beginning (e.g., "(transitive, ergative)")
        text = text.replaceAll("^\\([^)]*\\)\\s*", "");

        // Replace all whitespace (including newlines, tabs) with single space and trim
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    private String extractEtymology(Element languageSection) {
        // Find the first Etymology header after the language section
        Element currentElement = languageSection.nextElementSibling();
        while (currentElement != null && !isLanguageHeader(currentElement)) {
            if ((currentElement.tagName().equals("h3") || currentElement.tagName().equals("h4"))) {
                String headerText = currentElement.text();
                if (headerText.equals("Etymology") || headerText.startsWith("Etymology ")) {
                    String etymologyText = extractParagraphAfterHeader(currentElement);
                    if (!etymologyText.isEmpty()) {
                        return etymologyText;
                    }
                }
            }
            currentElement = currentElement.nextElementSibling();
        }
        return "";
    }

    private String extractParagraphAfterHeader(Element header) {
        Element nextElement = header.nextElementSibling();
        while (nextElement != null && !nextElement.tagName().matches(HEADER_REGEX)) {
            if (nextElement.tagName().equals("p")) {
                return nextElement.text();
            }
            nextElement = nextElement.nextElementSibling();
        }
        return "";
    }

    private List<String> extractSynonyms(Element languageSection, String partOfSpeech) {
        List<String> synonyms = new ArrayList<>();

        Element posHeader = findPartOfSpeechHeader(languageSection, partOfSpeech);
        if (posHeader == null) {
            return synonyms;
        }

        Element meaningsList = findFirstOrderedListAfterHeader(posHeader);
        if (meaningsList == null) {
            return synonyms;
        }

        extractSynonymsFromFirstMeaning(meaningsList, synonyms);

        return synonyms;
    }

    private void extractSynonymsFromFirstMeaning(Element meaningsList, List<String> synonyms) {
        // Navigate to the same deepest level as extractMeaningsFromList does
        Element deepestOl = navigateToDeepestLevel(meaningsList);
        if (deepestOl == null) {
            return;
        }

        // Find the first non-empty li at this deepest level
        Elements items = deepestOl.select("> li");
        for (Element li : items) {
            if (li.hasClass(EMPTY_ELEMENT_CLASS)) {
                continue;
            }

            // Look for synonyms in this first non-empty li
            Elements ddElements = li.select("dl dd");
            for (Element dd : ddElements) {
                if (processSynonymElement(dd, synonyms)) {
                    return; // Found synonyms, we're done
                }
            }
            return; // Checked first non-empty li, no synonyms found
        }
    }

    private Element navigateToDeepestLevel(Element ol) {
        // Follow the same logic as extractMeaningsFromList
        // Check if the first item has nested ols
        Elements topLevelItems = ol.select("> li");
        if (topLevelItems.isEmpty()) {
            return null;
        }

        Element firstItem = topLevelItems.first();
        Elements nestedOls = firstItem.select("> ol");

        if (!nestedOls.isEmpty()) {
            // Has nested structure - go deeper recursively
            return navigateToDeepestLevel(nestedOls.first());
        }
        else {
            // This is the deepest level
            return ol;
        }
    }

    private boolean processSynonymElement(Element dd, List<String> synonyms) {
        String ddText = dd.text();
        if (!ddText.startsWith("Synonym:") && !ddText.startsWith("Synonyms:")) {
            return false;
        }

        // Extract synonym spans directly from the dd element
        // The full text contains synonyms separated by commas, then a semicolon before "see also"
        // We want to stop at the semicolon
        String fullText = dd.text();
        int semicolonIndex = fullText.indexOf(';');

        // Create the text portion we want to extract from (before the semicolon)
        String synonymsText = semicolonIndex >= 0 ? fullText.substring(0, semicolonIndex) : fullText;

        // Extract all spans with lang=en
        Elements synonymSpans = dd.select("span");
        for (Element span : synonymSpans) {
            String synonym = span.text().trim();

            // Skip empty synonyms and structural elements
            if (synonym.isEmpty() || synonym.equals("Synonyms:") || synonym.equals("Synonym:")) {
                continue;
            }

            // Check if this synonym appears in the synonymsText (before semicolon) as a complete word
            // Pattern: the synonym should be preceded by "Synonyms: " or ", " and followed by ", " or end of string
            String pattern = "(Synonyms?: |, )" + Pattern.quote(synonym) + "(, |$)";
            if (Pattern.compile(pattern).matcher(synonymsText).find()) {
                synonyms.add(synonym);
            }
        }

        return true;
    }
}
