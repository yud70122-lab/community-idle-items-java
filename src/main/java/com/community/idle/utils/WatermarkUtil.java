package com.community.idle.utils;

import com.community.idle.config.WatermarkProperties;
import com.community.idle.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

@Slf4j
@Component
public class WatermarkUtil {

    private final WatermarkProperties watermarkProperties;

    public WatermarkUtil(WatermarkProperties watermarkProperties) {
        this.watermarkProperties = watermarkProperties;
    }

    @PostConstruct
    public void init() {
        File tempDir = new File(watermarkProperties.getTempDir());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    public InputStream addWatermark(MultipartFile file, String watermarkText) {
        if (!watermarkProperties.getEnabled()) {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new BusinessException("获取文件流失败");
            }
        }

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                return file.getInputStream();
            }

            BufferedImage watermarkedImage = addTextWatermark(originalImage, watermarkText);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            String formatName = getFormatName(file.getOriginalFilename());
            ImageIO.write(watermarkedImage, formatName, os);

            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("图片水印处理失败", e);
            try {
                return file.getInputStream();
            } catch (IOException ex) {
                throw new BusinessException("图片处理失败");
            }
        }
    }

    public InputStream addWatermarkWithLogo(MultipartFile file, String watermarkText, String logoPath) {
        if (!watermarkProperties.getEnabled()) {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new BusinessException("获取文件流失败");
            }
        }

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                return file.getInputStream();
            }

            BufferedImage watermarkedImage;
            if (logoPath != null && !logoPath.isEmpty()) {
                watermarkedImage = addImageWatermark(originalImage, logoPath, watermarkText);
            } else {
                watermarkedImage = addTextWatermark(originalImage, watermarkText);
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            String formatName = getFormatName(file.getOriginalFilename());
            ImageIO.write(watermarkedImage, formatName, os);

            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("图片水印处理失败", e);
            try {
                return file.getInputStream();
            } catch (IOException ex) {
                throw new BusinessException("图片处理失败");
            }
        }
    }

    public File addWatermarkToFile(MultipartFile file, String watermarkText) {
        try {
            InputStream inputStream = addWatermark(file, watermarkText);
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String tempFileName = System.currentTimeMillis() + "_wm" + extension;
            File tempFile = new File(watermarkProperties.getTempDir(), tempFileName);

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (Exception e) {
            log.error("水印处理生成临时文件失败", e);
            throw new BusinessException("图片处理失败");
        }
    }

    private BufferedImage addTextWatermark(BufferedImage sourceImage, String watermarkText) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) watermarkedImage.getGraphics();

        g2d.drawImage(sourceImage, 0, 0, width, height, null);

        String text = watermarkText != null && !watermarkText.isEmpty()
                ? watermarkText
                : watermarkProperties.getText();

        Font font = new Font("微软雅黑", Font.BOLD, calculateFontSize(width));
        g2d.setFont(font);

        Color color = Color.decode(watermarkProperties.getColor());
        AlphaComposite alphaComposite = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, watermarkProperties.getOpacity());
        g2d.setComposite(alphaComposite);
        g2d.setColor(color);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fontMetrics = g2d.getFontMetrics(font);
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();

        int[] position = calculatePosition(width, height, textWidth, textHeight);
        int x = position[0];
        int y = position[1];

        g2d.drawString(text, x, y);
        g2d.dispose();

        return watermarkedImage;
    }

    private BufferedImage addImageWatermark(BufferedImage sourceImage, String logoPath, String watermarkText) {
        try {
            int width = sourceImage.getWidth();
            int height = sourceImage.getHeight();

            BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) watermarkedImage.getGraphics();

            g2d.drawImage(sourceImage, 0, 0, width, height, null);

            BufferedImage logoImage;
            if (logoPath.startsWith("http")) {
                logoImage = ImageIO.read(new URL(logoPath));
            } else {
                logoImage = ImageIO.read(new File(logoPath));
            }

            if (logoImage == null) {
                g2d.dispose();
                return addTextWatermark(sourceImage, watermarkText);
            }

            int logoWidth = Math.min(width / 6, 100);
            int logoHeight = logoWidth * logoImage.getHeight() / logoImage.getWidth();

            AlphaComposite alphaComposite = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, watermarkProperties.getOpacity());
            g2d.setComposite(alphaComposite);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int[] position = calculatePosition(width, height, logoWidth, logoHeight);
            int x = position[0];
            int y = position[1];

            g2d.drawImage(logoImage, x, y, logoWidth, logoHeight, null);

            if (watermarkText != null && !watermarkText.isEmpty()) {
                Font font = new Font("微软雅黑", Font.PLAIN, calculateFontSize(width) / 2);
                g2d.setFont(font);
                Color color = Color.decode(watermarkProperties.getColor());
                g2d.setColor(color);
                FontMetrics fontMetrics = g2d.getFontMetrics(font);
                int textWidth = fontMetrics.stringWidth(watermarkText);
                int textX = x + (logoWidth - textWidth) / 2;
                int textY = y + logoHeight + fontMetrics.getHeight() + 5;
                g2d.drawString(watermarkText, textX, textY);
            }

            g2d.dispose();
            return watermarkedImage;
        } catch (Exception e) {
            log.error("图片水印处理失败，降级为文字水印", e);
            return addTextWatermark(sourceImage, watermarkText);
        }
    }

    private int calculateFontSize(int imageWidth) {
        int baseSize = watermarkProperties.getFontSize();
        int calculatedSize = Math.max(imageWidth / 20, 12);
        return Math.min(calculatedSize, baseSize * 2);
    }

    private int[] calculatePosition(int imageWidth, int imageHeight, int elementWidth, int elementHeight) {
        int marginX = watermarkProperties.getMarginX();
        int marginY = watermarkProperties.getMarginY();
        String position = watermarkProperties.getPosition();

        int x, y;
        switch (position.toUpperCase()) {
            case "TOP_LEFT":
                x = marginX;
                y = marginY + elementHeight;
                break;
            case "TOP_RIGHT":
                x = imageWidth - elementWidth - marginX;
                y = marginY + elementHeight;
                break;
            case "BOTTOM_LEFT":
                x = marginX;
                y = imageHeight - marginY;
                break;
            case "CENTER":
                x = (imageWidth - elementWidth) / 2;
                y = (imageHeight + elementHeight) / 2;
                break;
            case "BOTTOM_RIGHT":
            default:
                x = imageWidth - elementWidth - marginX;
                y = imageHeight - marginY;
                break;
        }

        return new int[]{Math.max(0, x), Math.max(elementHeight, Math.min(imageHeight, y))};
    }

    public BufferedImage createTextWatermarkImage(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, watermarkProperties.getOpacity()));

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 24));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);

        g2d.dispose();
        return image;
    }

    public String getFormatName(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png":
                return "png";
            case "gif":
                return "gif";
            case "bmp":
                return "bmp";
            case "jpg":
            case "jpeg":
            default:
                return "jpg";
        }
    }

    public static class WatermarkPosition {
        public static final String TOP_LEFT = "TOP_LEFT";
        public static final String TOP_RIGHT = "TOP_RIGHT";
        public static final String BOTTOM_LEFT = "BOTTOM_LEFT";
        public static final String BOTTOM_RIGHT = "BOTTOM_RIGHT";
        public static final String CENTER = "CENTER";
    }
}
