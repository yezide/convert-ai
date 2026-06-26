package com.convertai.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class AppConfig {
    private final Properties properties;
    private final Path appHome;

    private AppConfig(Properties properties, Path appHome) {
        this.properties = properties;
        this.appHome = appHome;
    }

    public static AppConfig load() {
        Path appHome = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path configPath = appHome.resolve("config").resolve("application.properties");
        Properties properties = defaults();

        if (Files.exists(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            } catch (IOException e) {
                throw new IllegalStateException("读取配置文件失败: " + configPath, e);
            }
        }

        return new AppConfig(properties, appHome);
    }

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("dashscope.api-key", "");
        properties.setProperty("dashscope.compatible-chat-url", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        properties.setProperty("dashscope.image-generation-url", "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis");
        properties.setProperty("dashscope.task-query-url-prefix", "https://dashscope.aliyuncs.com/api/v1/tasks/");
        properties.setProperty("model.text", "qwen-max");
        properties.setProperty("model.vision", "qwen-vl-max");
        properties.setProperty("model.image", "wanx-v1");
        properties.setProperty("ffmpeg.path", "bin/ffmpeg.exe");
        properties.setProperty("video.watermark-text", "本片由AI生成，仅供娱乐");
        properties.setProperty("video.watermark-font-file", "C:/Windows/Fonts/msyh.ttc");
        properties.setProperty("video.watermark-x", "20");
        properties.setProperty("video.watermark-y", "20");
        properties.setProperty("video.output-width", "720");
        properties.setProperty("video.output-height", "1280");
        properties.setProperty("video.crf", "23");
        properties.setProperty("video.bitrate", "4500k");
        properties.setProperty("video.minrate", "4000k");
        properties.setProperty("video.maxrate", "5000k");
        properties.setProperty("video.bufsize", "8000k");
        properties.setProperty("video.preset", "medium");
        properties.setProperty("image.output-size", "720*1280");
        properties.setProperty("image.poll-interval-ms", "2000");
        properties.setProperty("image.poll-timeout-seconds", "180");
        properties.setProperty("image.title.enabled", "true");
        properties.setProperty("image.title.font-file", "C:/Windows/Fonts/STXINGKA.TTF");
        properties.setProperty("image.title.position", "top");
        properties.setProperty("image.title.margin-top", "80");
        properties.setProperty("image.title.max-width-ratio", "0.86");
        properties.setProperty("image.title.font-size-ratio", "0.075");
        properties.setProperty("image.title.font-color", "#F8E6B0");
        properties.setProperty("image.title.outline-color", "#3B1608");
        return properties;
    }

    public Path appHome() {
        return appHome;
    }

    public String apiKey() {
        return get("dashscope.api-key");
    }

    public String chatUrl() {
        return get("dashscope.compatible-chat-url");
    }

    public String imageGenerationUrl() {
        return get("dashscope.image-generation-url");
    }

    public String taskQueryUrlPrefix() {
        return get("dashscope.task-query-url-prefix");
    }

    public String textModel() {
        return get("model.text");
    }

    public String visionModel() {
        return get("model.vision");
    }

    public String imageModel() {
        return get("model.image");
    }

    public Path ffmpegPath() {
        Path configured = Path.of(get("ffmpeg.path"));
        return configured.isAbsolute() ? configured : appHome.resolve(configured).normalize();
    }

    public String watermarkText() {
        return get("video.watermark-text");
    }

    public String watermarkFontFile() {
        return get("video.watermark-font-file");
    }

    public int watermarkX() {
        return getInt("video.watermark-x");
    }

    public int watermarkY() {
        return getInt("video.watermark-y");
    }

    public int outputWidth() {
        return getInt("video.output-width");
    }

    public int outputHeight() {
        return getInt("video.output-height");
    }

    public int crf() {
        return getInt("video.crf");
    }

    public String bitrate() {
        return get("video.bitrate");
    }

    public String minrate() {
        return get("video.minrate");
    }

    public String maxrate() {
        return get("video.maxrate");
    }

    public String bufsize() {
        return get("video.bufsize");
    }

    public String preset() {
        return get("video.preset");
    }

    public String imageOutputSize() {
        return get("image.output-size");
    }

    public Duration imagePollInterval() {
        return Duration.ofMillis(getLong("image.poll-interval-ms"));
    }

    public Duration imagePollTimeout() {
        return Duration.ofSeconds(getLong("image.poll-timeout-seconds"));
    }

    public boolean imageTitleEnabled() {
        return Boolean.parseBoolean(get("image.title.enabled"));
    }

    public String imageTitleFontFile() {
        return get("image.title.font-file");
    }

    public String imageTitlePosition() {
        return get("image.title.position");
    }

    public int imageTitleMarginTop() {
        return getInt("image.title.margin-top");
    }

    public double imageTitleMaxWidthRatio() {
        return getDouble("image.title.max-width-ratio");
    }

    public double imageTitleFontSizeRatio() {
        return getDouble("image.title.font-size-ratio");
    }

    public String imageTitleFontColor() {
        return get("image.title.font-color");
    }

    public String imageTitleOutlineColor() {
        return get("image.title.outline-color");
    }

    private String get(String key) {
        return properties.getProperty(key, "").trim();
    }

    private int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    private long getLong(String key) {
        return Long.parseLong(get(key));
    }

    private double getDouble(String key) {
        return Double.parseDouble(get(key));
    }
}
