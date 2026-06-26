package com.convertai.service;

import com.convertai.config.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FfmpegService {
    private final AppConfig config;

    public FfmpegService(AppConfig config) {
        this.config = config;
    }

    public void transcodeTo1080pWithWatermark(Path input, Path output, LogSink logSink) throws IOException, InterruptedException {
        Path ffmpeg = config.ffmpegPath();
        if (!Files.exists(ffmpeg)) {
            throw new IOException("找不到 FFmpeg: " + ffmpeg + "，请将 ffmpeg.exe 放到配置指定位置");
        }

        Files.createDirectories(output.getParent());

        String drawText = "drawtext=text='" + escapeDrawText(config.watermarkText()) + "'"
                + fontFileExpression()
                + ":x=" + config.watermarkX()
                + ":y=" + config.watermarkY()
                + ":fontsize=32"
                + ":fontcolor=white"
                + ":box=1"
                + ":boxcolor=black@0.45"
                + ":boxborderw=8";
        String videoFilter = "scale=" + config.outputWidth() + ":" + config.outputHeight()
                + ":force_original_aspect_ratio=decrease,"
                + "pad=" + config.outputWidth() + ":" + config.outputHeight() + ":(ow-iw)/2:(oh-ih)/2,"
                + drawText;

        List<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-i");
        command.add(input.toString());
        command.add("-vf");
        command.add(videoFilter);
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(config.preset());
        command.add("-b:v");
        command.add(config.bitrate());
        command.add("-minrate");
        command.add(config.minrate());
        command.add("-maxrate");
        command.add(config.maxrate());
        command.add("-bufsize");
        command.add(config.bufsize());
        command.add("-x264-params");
        command.add("nal-hrd=cbr:force-cfr=1");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=") || line.contains("Error") || line.contains("error")) {
                    logSink.info("FFmpeg: " + line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg 转码失败，退出码: " + exitCode + "，文件: " + input);
        }
    }

    private String escapeDrawText(String text) {
        return text.replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'")
                .replace("%", "\\%");
    }

    private String fontFileExpression() {
        String fontFile = config.watermarkFontFile();
        if (fontFile.isBlank() || !Files.exists(Path.of(fontFile))) {
            return "";
        }
        return ":fontfile='" + escapeDrawText(fontFile.replace("\\", "/")) + "'";
    }
}
