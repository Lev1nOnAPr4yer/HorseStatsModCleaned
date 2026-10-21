package dev.gallon.domain;

import dev.gallon.CompareState;
import java.util.Locale;
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
        keys.add("horsestatsmod.breeding.title");
        keys.add(I18nKeys.DISPLAY_BREEDING_STATUS);
        for (BreedingStatus.State state : BreedingStatus.State.values()) {
            keys.add("horsestatsmod.breeding.state." + state.name().toLowerCase(Locale.ROOT));
        }
        for (BreedingStatus.Blocker blocker : BreedingStatus.Blocker.values()) {
            keys.add("horsestatsmod.breeding.detail." + blocker.name().toLowerCase(Locale.ROOT));
        }
        for (String detail : List.of("foal", "cooldown", "ready", "can_be_fed", "can_be_fed_estimated", "unknown")) {
            keys.add("horsestatsmod.breeding.detail." + detail);
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
