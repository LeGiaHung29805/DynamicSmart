package com.dynamicmart.catalog_service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchTermNormalizerTest {
    private final SearchTermNormalizer normalizer = new SearchTermNormalizer();

    @Test
    void removesVietnameseAccentsAndNormalizesSeparators() {
        SearchTerms terms = normalizer.normalize("  Áo-khoác   ĐẸP  ");

        assertEquals("ao khoac dep", terms.primary());
        assertTrue(terms.compact().contains("aokhoacdep"));
    }

    @Test
    void expandsConfiguredSynonymWithoutBrandLogic() {
        SearchTerms terms = normalizer.normalize("Áo phông nam");

        assertEquals("ao phong nam", terms.primary());
        assertTrue(terms.normalized().contains("ao thun nam"));
    }

    @Test
    void returnsEmptyTermsForBlankInput() {
        assertTrue(normalizer.normalize("   ").isEmpty());
    }
}
