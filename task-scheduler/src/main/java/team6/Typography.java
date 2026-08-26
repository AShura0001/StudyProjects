package team6;

import java.util.Set;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The two font families and the type scale used consistently across GanttChart, StatsPanel,
 * WorkerPanel and PlaybackBar, so the visualisation doesn't end up a mix of JavaFX defaults
 * and ad-hoc sizes. Families are resolved once, at class-load, against whatever fonts are
 * actually installed - the CI/marker's box may not have the same fonts as a dev machine.
 */
final class Typography {

    /** General UI text: titles, labels, everything that isn't a counter. */
    static final String UI_FAMILY = resolveFamily(Font.getDefault().getFamily(),
            "Segoe UI", "Inter", "Noto Sans", "DejaVu Sans", "Liberation Sans");

    /** Numbers and other counters, so digit width stays fixed and they don't jitter as they change. */
    static final String MONO_FAMILY = resolveFamily("Monospaced",
            "Cascadia Mono", "Consolas", "JetBrains Mono", "DejaVu Sans Mono", "Liberation Mono");

    private Typography() {
        // Static utility, not meant to be instantiated.
    }

    private static String resolveFamily(String fallback, String... preferredInOrder) {
        Set<String> available = Set.copyOf(Font.getFamilies());
        for (String candidate : preferredInOrder) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    static Font title() {
        return Font.font(UI_FAMILY, FontWeight.SEMI_BOLD, 18);
    }

    static Font subtitle() {
        return Font.font(UI_FAMILY, FontWeight.NORMAL, 12);
    }

    static Font sectionLabel() {
        return Font.font(UI_FAMILY, FontWeight.SEMI_BOLD, 11);
    }

    static Font statValue() {
        return Font.font(MONO_FAMILY, FontWeight.NORMAL, 18);
    }

    static Font rowLabel() {
        return Font.font(UI_FAMILY, FontWeight.SEMI_BOLD, 12);
    }

    static Font barLabel() {
        return Font.font(UI_FAMILY, FontWeight.SEMI_BOLD, 11);
    }

    static Font axisTick() {
        return Font.font(UI_FAMILY, FontWeight.NORMAL, 11);
    }

    static Font workerBody() {
        return Font.font(MONO_FAMILY, FontWeight.NORMAL, 12);
    }
}
