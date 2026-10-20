package dev.gallon.domain;

import dev.gallon.CompareState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationsTest {
    @Test
    void everyCompareTextIsTranslatedInEnglishAndFrench() throws IOException {
        List<String> keys = new ArrayList<>(List.of(
                "horsestatsmod.compare.title",
                "horsestatsmod.compare.add_first",
                "horsestatsmod.compare.add_second",
                "horsestatsmod.compare.run",
                "horsestatsmod.compare.hint",
                "horsestatsmod.compare.results"
        ));
        for (CompareState.Problem problem : CompareState.Problem.values()) {
            keys.add(problem.translationKey());
        }

        for (String language : List.of("en_us", "fr_fr")) {
            String translations = read("/assets/horsestatsmod/lang/" + language + ".json");
            for (String key : keys) {
                assertTrue(translations.contains("\"" + key + "\""), language + " is missing " + key);
                assertFalse(translations.contains("\"" + key + "\": \"\""), language + " has an empty " + key);
            }
        }
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = TranslationsTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing classpath resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
