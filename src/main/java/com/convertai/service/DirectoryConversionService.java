package com.convertai.service;

import com.convertai.config.AppConfig;
import com.convertai.model.EpisodeInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class DirectoryConversionService {
    private final AppConfig config;
    private final AliBailianClient aiClient;
    private final FfmpegService ffmpegService;

    public DirectoryConversionService(AppConfig config, AliBailianClient aiClient, FfmpegService ffmpegService) {
        this.config = config;
        this.aiClient = aiClient;
        this.ffmpegService = ffmpegService;
    }

    public void convert(Path selectedDirectory, LogSink logSink) throws IOException, InterruptedException {
        if (!Files.isDirectory(selectedDirectory)) {
            throw new IllegalArgumentException("请选择有效目录: " + selectedDirectory);
        }

        Path outputDirectory = selectedDirectory.resolve("convert");
        Files.createDirectories(outputDirectory);
        logSink.info("输出目录: " + outputDirectory);

        List<Path> files = listDirectFiles(selectedDirectory);
        List<Path> textFiles = files.stream().filter(FileClassifier::isText).sorted().toList();
        List<Path> imageFiles = files.stream().filter(FileClassifier::isImage).sorted().toList();
        List<Path> videoFiles = sortVideos(files.stream().filter(FileClassifier::isVideo).toList());

        EpisodeInfo episodeInfo = processIntro(textFiles, outputDirectory, logSink);
        processImages(imageFiles, outputDirectory, episodeInfo, logSink);
        processVideos(videoFiles, outputDirectory, episodeInfo.title(), logSink);

        logSink.info("全部处理完成。");
    }

    private List<Path> listDirectFiles(Path selectedDirectory) throws IOException {
        try (Stream<Path> stream = Files.list(selectedDirectory)) {
            return stream
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase("convert"))
                    .filter(Files::isRegularFile)
                    .toList();
        }
    }

    private EpisodeInfo processIntro(List<Path> textFiles, Path outputDirectory, LogSink logSink) throws IOException, InterruptedException {
        if (textFiles.isEmpty()) {
            logSink.info("未找到文本简介，使用默认剧集信息。");
            return new EpisodeInfo("未命名剧集", "");
        }

        Path introFile = textFiles.get(0);
        logSink.info("读取简介: " + introFile.getFileName());
        String originalText = readText(introFile);
        EpisodeInfo episodeInfo = aiClient.rewriteIntro(originalText);
        Path output = outputDirectory.resolve(introFile.getFileName());
        Files.writeString(output, episodeInfo.intro(), StandardCharsets.UTF_8);
        logSink.info("生成新简介: " + output.getFileName() + "，剧集名: " + episodeInfo.title());

        for (int i = 1; i < textFiles.size(); i++) {
            Path otherText = textFiles.get(i);
            Files.copy(otherText, outputDirectory.resolve(otherText.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            logSink.info("复制其他文本文件: " + otherText.getFileName());
        }
        return episodeInfo;
    }

    private void processImages(List<Path> imageFiles, Path outputDirectory, EpisodeInfo episodeInfo, LogSink logSink) throws IOException, InterruptedException {
        if (imageFiles.isEmpty()) {
            logSink.info("未找到封面图片，跳过图片生成。");
            return;
        }

        for (Path imageFile : imageFiles) {
            logSink.info("分析封面: " + imageFile.getFileName());
            String prompt = aiClient.analyzeImage(imageFile, episodeInfo.title(), episodeInfo.intro());
            Path output = outputDirectory.resolve(replaceExtension(imageFile.getFileName().toString(), "png"));
            logSink.info("生成新封面: " + output.getFileName());
            aiClient.generateImage(prompt, output);
        }
    }

    private void processVideos(List<Path> videoFiles, Path outputDirectory, String episodeTitle, LogSink logSink) throws IOException, InterruptedException {
        if (videoFiles.isEmpty()) {
            logSink.info("未找到视频文件，跳过视频处理。");
            return;
        }

        AtomicInteger fallback = new AtomicInteger(1);
        for (Path videoFile : videoFiles) {
            int index = EpisodeIndexExtractor.extractIndexOrFallback(videoFile, fallback.getAndIncrement());
            String outputName = episodeTitle + "-" + index + "." + FileClassifier.extension(videoFile);
            Path output = outputDirectory.resolve(outputName);
            logSink.info("处理视频: " + videoFile.getFileName() + " -> " + output.getFileName());
            ffmpegService.transcodeTo1080pWithWatermark(videoFile, output, logSink);
        }
    }

    private List<Path> sortVideos(List<Path> videoFiles) {
        AtomicInteger fallback = new AtomicInteger(1);
        return videoFiles.stream()
                .map(path -> new IndexedVideo(path, EpisodeIndexExtractor.extractIndexOrFallback(path, fallback.getAndIncrement())))
                .sorted(Comparator.comparingInt(IndexedVideo::index).thenComparing(video -> video.path().getFileName().toString()))
                .map(IndexedVideo::path)
                .toList();
    }

    private String replaceExtension(String fileName, String extension) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return baseName + "." + extension;
    }

    private String readText(Path path) throws IOException {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readString(path, Charset.forName("GBK"));
        }
    }

    private record IndexedVideo(Path path, int index) {
    }
}
