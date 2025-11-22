package util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

public class ImageConverter {

    // --- FIX: Removed ".webp" from this list ---
    private static final String[] UNSUPPORTED_EXTENSIONS = {".avif", ".tiff", ".heic"};

    private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    public static String getCompatibleImageUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return originalUrl;
        String cleanUrl = originalUrl;

        // Discord fix
        if (!cleanUrl.contains("discordapp.net") && !cleanUrl.contains("media.discordapp.net")) {
            int queryIndex = cleanUrl.indexOf('?');
            if (queryIndex != -1) cleanUrl = cleanUrl.substring(0, queryIndex);
        }

        // Wikia fix
        if (cleanUrl.contains("wikia.nocookie.net")) {
            String lowerUrl = cleanUrl.toLowerCase();
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (lowerUrl.contains(ext)) {
                    int extIndex = lowerUrl.lastIndexOf(ext);
                    return cleanUrl.substring(0, extIndex + ext.length());
                }
            }
        }
        return cleanUrl;
    }

    public static boolean isWebP(String url) {
        return url != null && url.toLowerCase().endsWith(".webp");
    }

    public static boolean isWebM(String url) {
        return url != null && url.toLowerCase().endsWith(".webm");
    }

    public static boolean isUnsupportedFormat(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();

        // Explicitly allow WebP now
        if (lowerUrl.endsWith(".webp")) return false;

        for (String ext : UNSUPPORTED_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Downloads a WebP image, converts it to PNG, and returns the local file path.
     */
    public static String convertWebPtoPng(String webpUrl) {
        try {
            URL url = new URL(webpUrl);
            // ImageIO uses the TwelveMonkeys plugin to read WebP
            BufferedImage image = ImageIO.read(url);

            if (image == null) return webpUrl; // Failed to decode

            // Create a temp file
            File tempFile = File.createTempFile("vop_img_", ".png");
            tempFile.deleteOnExit();

            // Save as PNG
            ImageIO.write(image, "png", tempFile);

            // Return the local file URI
            return tempFile.toURI().toString();

        } catch (Exception e) {
            e.printStackTrace();
            return webpUrl; // Return original on failure
        }
    }
}
