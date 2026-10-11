package com.dynamicmart.catalog_service.query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SearchTermNormalizer {
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_ALTERNATIVES = 8;

    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "ao phong", List.of("ao thun"),
            "ao thun", List.of("ao phong"),
            "dam", List.of("vay"),
            "vay", List.of("dam"),
            "jacket", List.of("ao khoac"),
            "ao khoac", List.of("jacket"),
            "sneaker", List.of("giay the thao"),
            "giay the thao", List.of("sneaker")
    );

    public SearchTerms normalize(String rawKeyword) {
        String primary = normalizeText(rawKeyword);
        if (primary.isBlank()) return SearchTerms.empty();

        Set<String> alternatives = new LinkedHashSet<>();
        alternatives.add(primary);
        SYNONYMS.forEach((source, replacements) -> {
            if (containsPhrase(primary, source)) {
                replacements.stream()
                        .map(replacement -> replacePhrase(primary, source, replacement))
                        .forEach(alternatives::add);
            }
        });

        List<String> normalized = alternatives.stream()
                .filter(value -> !value.isBlank())
                .limit(MAX_ALTERNATIVES)
                .toList();
        List<String> compact = normalized.stream()
                .map(value -> value.replace(" ", ""))
                .distinct()
                .toList();
        return new SearchTerms(normalized, compact);
    }

    public String normalizeText(String value) {
        if (value == null || value.isBlank()) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String ascii = COMBINING_MARKS.matcher(decomposed).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return WHITESPACE.matcher(SEPARATORS.matcher(ascii).replaceAll(" ").trim())
                .replaceAll(" ");
    }

    private boolean containsPhrase(String value, String phrase) {
        return (" " + value + " ").contains(" " + phrase + " ");
    }

    private String replacePhrase(String value, String source, String replacement) {
        List<String> words = new ArrayList<>(List.of(value.split(" ")));
        List<String> sourceWords = List.of(source.split(" "));
        for (int index = 0; index <= words.size() - sourceWords.size(); index++) {
            if (words.subList(index, index + sourceWords.size()).equals(sourceWords)) {
                List<String> replaced = new ArrayList<>(words.subList(0, index));
                replaced.addAll(List.of(replacement.split(" ")));
                replaced.addAll(words.subList(index + sourceWords.size(), words.size()));
                return String.join(" ", replaced);
            }
        }
        return value;
    }
}
