package com.convertai.service;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class FileClassifier {
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "bmp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mkv", "mov", "avi", "flv", "wmv", "m4v");

    private FileClassifier() {
    }

    public static boolean isText(Path path) {
        return TEXT_EXTENSIONS.contains(extension(path));
    }

    public static boolean isImage(Path path) {
        return IMAGE_EXTENSIONS.contains(extension(path));
    }

    public static boolean isVideo(Path path) {
        return VIDEO_EXTENSIONS.contains(extension(path));
    }

    public static String extension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
