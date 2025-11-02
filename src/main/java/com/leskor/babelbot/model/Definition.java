package com.leskor.babelbot.model;

import java.util.List;

public record Definition(String term, String partOfSpeech, List<String> meanings, String etymology,
        List<String> synonyms) {
}
