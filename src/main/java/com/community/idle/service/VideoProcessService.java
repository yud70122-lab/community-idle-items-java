package com.community.idle.service;

import com.community.idle.config.WatermarkProperties;
import com.community.idle.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoProcessService {

    private final WatermarkProperties watermarkProperties;

    public VideoProcessService(WatermarkProperties watermarkProperties) {
        this.watermarkProperties = watermarkProperties;
    }

    @PostConstruct
    public void init() {
        File tempDir = new File(watermarkProperties.getTempDir());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        checkFFmpegAvailable();
    }

    public boolean checkFFmpegAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(watermarkProperties.getFfmpegPath() + " -version");
            boolean success = process.waitFor(5, TimeUnit.SECONDS);
            if (success && process.exitValue() == 0) {
                log.info("FFmpeg 检测可用");
                return true;
            }
        } catch (Exception e) {
            log.warn("FFmpeg 不可用，视频水印功能将无法使用: {}", e.getMessage());
        }
        return false;
    }

    @Async
    public void processVideoWithWatermark(MultipartFile videoFile, String watermarkText, VideoProcessCallback callback) {
        try {
            String resultPath = addWatermarkToVideo(videoFile, watermarkText);
            if (callback != null) {
                callback.onSuccess(resultPath);
            }
        } catch (Exception e) {
            log.error("视频水印处理失败", e);
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    public String addWatermarkToVideo(MultipartFile videoFile, String watermarkText) throws Exception {
        if (!watermarkProperties.getEnabled()) {
            return saveVideoFile(videoFile);
        }

        String inputPath = saveTempVideoFile(videoFile);
        String outputPath = generateOutputPath(videoFile.getOriginalFilename());

        String watermarkContent = watermarkText != null && !watermarkText.isEmpty()
                ? watermarkText
                : watermarkProperties.getText();

        String ffmpegCommand = buildFFmpegCommand(inputPath, outputPath, watermarkContent);

        log.info("执行FFmpeg命令: {}", ffmpegCommand);

        int exitCode = executeCommand(ffmpegCommand);

        if (exitCode != 0) {
            log.error("视频水印处理失败，exitCode={}", exitCode);
            return saveVideoFile(videoFile);
        }

        log.info("视频水印处理完成，输出路径: {}", outputPath);

        Files.deleteIfExists(Paths.get(inputPath));

        return outputPath;
    }

    public String addWatermarkToVideoWithLogo(MultipartFile videoFile, String watermarkText, String logoPath) throws Exception {
        if (!watermarkProperties.getEnabled()) {
            return saveVideoFile(videoFile);
        }

        String inputPath = saveTempVideoFile(videoFile);
        String outputPath = generateOutputPath(videoFile.getOriginalFilename());

        String watermarkContent = watermarkText != null && !watermarkText.isEmpty()
                ? watermarkText
                : watermarkProperties.getText();

        String ffmpegCommand;
        if (logoPath != null && !logoPath.isEmpty()) {
            ffmpegCommand = buildFFmpegCommandWithLogo(inputPath, outputPath, logoPath, watermarkContent);
        } else {
            ffmpegCommand = buildFFmpegCommand(inputPath, outputPath, watermarkContent);
        }

        log.info("执行FFmpeg命令: {}", ffmpegCommand);

        int exitCode = executeCommand(ffmpegCommand);

        if (exitCode != 0) {
            log.error("视频水印处理失败，exitCode={}", exitCode);
            return saveVideoFile(videoFile);
        }

        log.info("视频水印处理完成，输出路径: {}", outputPath);

        Files.deleteIfExists(Paths.get(inputPath));

        return outputPath;
    }

    private String buildFFmpegCommand(String inputPath, String outputPath, String watermarkText) {
        String position = getFFmpegPosition();
        String fontFamily = getFontFamily();
        float opacity = watermarkProperties.getOpacity();
        int fontSize = watermarkProperties.getFontSize();
        String color = watermarkProperties.getColor().replace("#", "");
        int marginX = watermarkProperties.getMarginX();
        int marginY = watermarkProperties.getMarginY();

        String drawTextFilter = String.format(
                "drawtext=fontfile=%s:text='%s':fontsize=%d:fontcolor=%s@%.2f:x=%s:y=%s:box=1:boxcolor=black@0.5:boxborderw=5",
                escapePath(fontFamily),
                escapeText(watermarkText),
                fontSize,
                color,
                opacity,
                position.replace("MARGIN_X", String.valueOf(marginX)).replace("MARGIN_Y", String.valueOf(marginY)),
                position.replace("MARGIN_X", String.valueOf(marginX)).replace("MARGIN_Y", String.valueOf(marginY)).replace("w", "h")
        );

        return String.format(
                "%s -i \"%s\" -vf \"%s\" -codec:v libx264 -codec:a copy -y \"%s\"",
                watermarkProperties.getFfmpegPath(),
                inputPath,
                drawTextFilter,
                outputPath
        );
    }

    private String buildFFmpegCommandWithLogo(String inputPath, String outputPath, String logoPath, String watermarkText) {
        String position = getFFmpegPosition();
        int marginX = watermarkProperties.getMarginX();
        int marginY = watermarkProperties.getMarginY();
        float opacity = watermarkProperties.getOpacity();

        String xExpr = position.split(":")[0].replace("MARGIN_X", String.valueOf(marginX));
        String yExpr = position.split(":")[1].replace("MARGIN_Y", String.valueOf(marginY));

        String filterComplex = String.format(
                "[1:v]scale=100:-1,format=rgba,colorchannelmixer=aa=%.2f[logo];" +
                        "[0:v][logo]overlay=%s:%s[v];" +
                        "[v]drawtext=fontfile=%s:text='%s':fontsize=24:fontcolor=white@%.2f:x=%s:y=%s+110",
                opacity,
                xExpr, yExpr,
                escapePath(getFontFamily()),
                escapeText(watermarkText),
                opacity,
                xExpr, yExpr
        );

        return String.format(
                "%s -i \"%s\" -i \"%s\" -filter_complex \"%s\" -map \"[v]\" -map 0:a -codec:v libx264 -codec:a copy -y \"%s\"",
                watermarkProperties.getFfmpegPath(),
                inputPath,
                logoPath,
                filterComplex,
                outputPath
        );
    }

    private String getFFmpegPosition() {
        int marginX = watermarkProperties.getMarginX();
        int marginY = watermarkProperties.getMarginY();
        String position = watermarkProperties.getPosition();

        switch (position.toUpperCase()) {
            case "TOP_LEFT":
                return String.format("x=%d:y=%d", marginX, marginY);
            case "TOP_RIGHT":
                return String.format("x=w-tw-%d:y=%d", marginX, marginY);
            case "BOTTOM_LEFT":
                return String.format("x=%d:y=h-th-%d", marginX, marginY);
            case "CENTER":
                return "x=(w-text_w)/2:y=(h-text_h)/2";
            case "BOTTOM_RIGHT":
            default:
                return String.format("x=w-tw-%d:y=h-th-%d", marginX, marginY);
        }
    }

    private String getFontFamily() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "C:/Windows/Fonts/msyh.ttc";
        } else if (os.contains("mac")) {
            return "/System/Library/Fonts/PingFang.ttc";
        } else {
            return "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf";
        }
    }

    private String escapePath(String path) {
        return path.replace("\\", "\\\\");
    }

    private String escapeText(String text) {
        return text.replace("'", "\\'").replace(":", "\\:");
    }

    private int executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("bash", "-c", command);
        }
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.contains("frame=") && line.contains("fps=")) {
                    log.debug("FFmpeg进度: {}", line.trim());
                }
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            log.error("视频处理超时，已强制终止");
            return -1;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("FFmpeg输出: {}", output.toString());
        }

        return exitCode;
    }

    private String saveTempVideoFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".mp4";
        String tempFileName = UUID.randomUUID().toString().replace("-", "") + "_input" + extension;
        Path tempPath = Paths.get(watermarkProperties.getTempDir(), tempFileName);
        file.transferTo(tempPath.toFile());
        return tempPath.toAbsolutePath().toString();
    }

    private String saveVideoFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".mp4";
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path filePath = Paths.get(watermarkProperties.getTempDir(), fileName);
        file.transferTo(filePath.toFile());
        return filePath.toAbsolutePath().toString();
    }

    private String generateOutputPath(String originalFilename) {
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".mp4";
        String fileName = UUID.randomUUID().toString().replace("-", "") + "_wm" + extension;
        return Paths.get(watermarkProperties.getTempDir(), fileName).toAbsolutePath().toString();
    }

    public VideoInfo getVideoInfo(String videoPath) {
        try {
            String command = String.format("%s -i \"%s\" 2>&1", watermarkProperties.getFfmpegPath(), videoPath);
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("bash", "-c", command);
            }
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor(5, TimeUnit.SECONDS);
            String info = output.toString();

            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setPath(videoPath);

            String durationMatch = "Duration: (\\d+):(\\d+):(\\d+\\.\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(durationMatch);
            java.util.regex.Matcher matcher = pattern.matcher(info);
            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                double seconds = Double.parseDouble(matcher.group(3));
                videoInfo.setDuration(hours * 3600 + minutes * 60 + seconds);
            }

            String resolutionMatch = "(\\d{3,4})x(\\d{3,4})";
            pattern = java.util.regex.Pattern.compile(resolutionMatch);
            matcher = pattern.matcher(info);
            if (matcher.find()) {
                videoInfo.setWidth(Integer.parseInt(matcher.group(1)));
                videoInfo.setHeight(Integer.parseInt(matcher.group(2)));
            }

            return videoInfo;
        } catch (Exception e) {
            log.error("获取视频信息失败", e);
            return null;
        }
    }

    public interface VideoProcessCallback {
        void onSuccess(String resultPath);

        void onError(String errorMessage);
    }

    public static class VideoInfo {
        private String path;
        private double duration;
        private int width;
        private int height;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }
}
