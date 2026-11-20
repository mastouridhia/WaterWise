package com.waterwise.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Perenual care data into values the app uses. Pure Java (no
 * Android classes), so it can be checked with simple unit tests.
 */
public final class CareMapper {

    public static final int DEFAULT_DAYS = 7;

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private CareMapper() {
    }

    /**
     * Benchmark like "7-10" (sometimes with extra quotes) → average of the
     * numbers, here 9 (rounded). Returns null if there is no number.
     */
    public static Integer daysFromBenchmark(String value) {
        if (value == null) return null;
        Matcher m = NUMBER.matcher(value);
        int sum = 0;
        int count = 0;
        while (m.find()) {
            sum += Integer.parseInt(m.group());
            count++;
        }
        if (count == 0) return null;
        int days = Math.round((float) sum / count);
        return days >= 1 && days <= 90 ? days : null;
    }

    /** Perenual watering level → number of days between waterings. */
    public static int daysFromLevel(String level) {
        if (level == null) return DEFAULT_DAYS;
        switch (level.trim().toLowerCase(Locale.ROOT)) {
            case "frequent":
                return 3;
            case "average":
                return 7;
            case "minimum":
                return 14;
            case "none":
                return 30;
            default:
                return DEFAULT_DAYS;
        }
    }

    /** Uses the precise benchmark when present, otherwise the level. */
    public static int wateringDays(String benchmark, String level) {
        Integer days = daysFromBenchmark(benchmark);
        return days != null ? days : daysFromLevel(level);
    }

    /** ["full sun", "part shade"] → "Full sun, Part shade". */
    public static String formatSunlight(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) continue;
            String s = v.trim();
            parts.add(s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /** "swiss cheese plant" → "Swiss cheese plant". */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
