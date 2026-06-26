package com.convertai.service;

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EpisodeIndexExtractor {
    private static final Pattern[] INDEX_PATTERNS = {
            Pattern.compile("(?i)(?:第\\s*)?(\\d{1,4})\\s*(?:集|话|話|episode|ep)"),
            Pattern.compile("[\\[【(（]\\s*(\\d{1,4})\\s*[\\]】)）]"),
            Pattern.compile("(?:^|[-_\\s.])0*(\\d{1,4})(?=\\D*$)")
    };
    private static final Pattern ANY_NUMBER = Pattern.compile("\\d{1,4}");

    private EpisodeIndexExtractor() {
    }

    public static int extractIndexOrFallback(Path path, int fallback) {
        return extractIndex(path.getFileName().toString()).orElse(fallback);
    }

    public static OptionalInt extractIndex(String fileName) {
        String nameWithoutExtension = removeExtension(fileName);
        for (Pattern pattern : INDEX_PATTERNS) {
            Matcher matcher = pattern.matcher(nameWithoutExtension);
            if (matcher.find()) {
                return OptionalInt.of(Integer.parseInt(matcher.group(1)));
            }
        }

        Matcher matcher = ANY_NUMBER.matcher(nameWithoutExtension);
        OptionalInt lastNumber = OptionalInt.empty();
        while (matcher.find()) {
            lastNumber = OptionalInt.of(Integer.parseInt(matcher.group()));
        }
        return lastNumber;
    }

    private static String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
