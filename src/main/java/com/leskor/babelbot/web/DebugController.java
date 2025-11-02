package com.leskor.babelbot.web;

import com.leskor.babelbot.model.Definition;
import com.leskor.babelbot.service.DefinitionService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("debug")
public class DebugController {
    private final DefinitionService definitionService;

    public DebugController(DefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @PostMapping(path = "define", produces = "application/json")
    public DefineResponse defineWord(@RequestBody DefineRequest request) {
        return DefineResponse.fromDefinition(definitionService.getDefinition(request.term())
                .orElseThrow(() -> new IllegalStateException("Cannot obtain definition for term: " + request.term())));
    }

    record DefineRequest(String term) {
    }

    record DefineResponse(String partOfSpeech, String etymology, List<String> meanings, List<String> synonyms) {
        public static DefineResponse fromDefinition(Definition definition) {
            return new DefineResponse(definition.partOfSpeech(), definition.etymology(), definition.meanings(),
                    definition.synonyms());
        }
    }
}
