package com.convertai.service;

import com.convertai.config.AppConfig;
import com.convertai.model.EpisodeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class AliBailianClient {
    private final AppConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AliBailianClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public EpisodeInfo rewriteIntro(String originalText) throws IOException, InterruptedException {
        ensureApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.textModel());
        body.put("temperature", 0.7);
        body.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是影视剧宣发文案专家。请根据原简介判断剧集名字，改写一个新的剧集简介。必须只输出JSON，字段为title和intro。intro必须简洁有吸引力，整体字数控制在160个到200个中文字符。");
        messages.addObject()
                .put("role", "user")
                .put("content", "原简介如下：\n" + originalText);

        JsonNode response = postJson(config.chatUrl(), body);
        String content = response.at("/choices/0/message/content").asText();
        JsonNode parsed = objectMapper.readTree(content);
        String title = sanitizeFileName(parsed.path("title").asText("未命名剧集"));
        String intro = limitLength(parsed.path("intro").asText(originalText), 100);
        return new EpisodeInfo(title.isBlank() ? "未命名剧集" : title, intro);
    }

    public String analyzeImage(Path imagePath, String episodeTitle, String intro) throws IOException, InterruptedException {
        ensureApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.visionModel());
        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是影视短剧封面设计师，请分析封面构图、主体、风格、色彩和文字信息，输出适合图片生成模型的中文提示词。提示词必须要求新封面包含剧集名字，标题字体要飘逸、有高级手写感或书法感，文字清晰可读。");

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        content.addObject()
                .put("type", "text")
                .put("text", "剧集名：" + episodeTitle
                        + "\n新简介：" + intro
                        + "\n请结合图片内容输出一段新版竖屏短剧封面生成提示词。"
                        + "\n硬性要求：封面必须出现剧集名字《" + episodeTitle + "》，标题字体现代飘逸、灵动、有书法或手写笔触质感，文字清晰醒目，不要错别字。");
        content.addObject()
                .put("type", "image_url")
                .set("image_url", objectMapper.createObjectNode().put("url", toDataUrl(imagePath)));

        JsonNode response = postJson(config.chatUrl(), body);
        return response.at("/choices/0/message/content").asText()
                + "\n\n封面文字硬性要求：画面中必须包含剧集名字《" + episodeTitle
                + "》，标题放在视觉焦点区域，使用飘逸灵动的中文艺术字体、手写书法质感，清晰可读，无错别字。";
    }

    public void generateImage(String prompt, Path outputPath) throws IOException, InterruptedException {
        ensureApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.imageModel());
        body.set("input", objectMapper.createObjectNode().put("prompt", prompt));
        body.set("parameters", objectMapper.createObjectNode()
                .put("size", config.imageOutputSize())
                .put("n", 1));

        HttpRequest request = baseRequest(config.imageGenerationUrl())
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        JsonNode submitResponse = sendJson(request);
        String taskId = submitResponse.at("/output/task_id").asText();
        if (taskId.isBlank()) {
            throw new IOException("图片生成任务创建失败: " + submitResponse);
        }

        String imageUrl = waitForImageUrl(taskId);
        download(imageUrl, outputPath);
    }

    private String waitForImageUrl(String taskId) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(config.imagePollTimeout());
        while (Instant.now().isBefore(deadline)) {
            HttpRequest request = baseRequest(config.taskQueryUrlPrefix() + taskId).GET().build();
            JsonNode response = sendJson(request);
            String status = response.at("/output/task_status").asText();
            if ("SUCCEEDED".equalsIgnoreCase(status)) {
                String imageUrl = response.at("/output/results/0/url").asText();
                if (imageUrl.isBlank()) {
                    throw new IOException("图片生成成功但未返回图片地址: " + response);
                }
                return imageUrl;
            }
            if ("FAILED".equalsIgnoreCase(status) || "UNKNOWN".equalsIgnoreCase(status)) {
                throw new IOException("图片生成失败: " + response);
            }
            Thread.sleep(config.imagePollInterval().toMillis());
        }
        throw new IOException("图片生成超时，taskId=" + taskId);
    }

    private JsonNode postJson(String url, JsonNode body) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(url)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        return sendJson(request);
    }

    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private JsonNode sendJson(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private void download(String url, Path outputPath) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("下载图片失败 HTTP " + response.statusCode());
        }
        Files.write(outputPath, response.body());
    }

    private String toDataUrl(Path imagePath) throws IOException {
        String extension = FileClassifier.extension(imagePath);
        String mime = switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/png";
        };
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        return "data:" + mime + ";base64," + base64;
    }

    private void ensureApiKey() {
        if (config.apiKey().isBlank() || config.apiKey().contains("请填写")) {
            throw new IllegalStateException("请先在 config/application.properties 中填写 dashscope.api-key");
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    private String limitLength(String text, int maxCodePoints) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", "");
        int count = normalized.codePointCount(0, normalized.length());
        if (count <= maxCodePoints) {
            return normalized;
        }
        int endIndex = normalized.offsetByCodePoints(0, maxCodePoints);
        return normalized.substring(0, endIndex);
    }
}
