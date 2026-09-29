import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class MakeBackgroundTransparent {
    public static void main(String[] args) throws Exception {
        Path inputDir = Path.of("images");
        Path outputDir = Path.of("images", "transparent");
        Files.createDirectories(outputDir);

        int threshold = 230; // 0..255; higher = stricter "white"
        int softness = 75;   // alpha ramp; higher = smoother edges

        Files.list(inputDir)
                .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                .forEach(p -> {
                    try {
                        BufferedImage src = ImageIO.read(p.toFile());
                        BufferedImage out = new BufferedImage(
                                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);

                        for (int y = 0; y < src.getHeight(); y++) {
                            for (int x = 0; x < src.getWidth(); x++) {
                                int rgb = src.getRGB(x, y);

                                int r = (rgb >> 16) & 0xFF;
                                int g = (rgb >> 8) & 0xFF;
                                int b = rgb & 0xFF;

                                // "Whiteness" measure: near-white if all channels are high
                                int min = Math.min(r, Math.min(g, b));
                                int alpha;

                                if (min >= threshold) {
                                    // Fully transparent for near-white
                                    alpha = 0;
                                } else if (min >= threshold - softness) {
                                    // Soft edge: fade alpha between 0..255
                                    int t = (threshold - min); // 1..softness
                                    alpha = (int) (255.0 * (t / (double) softness));
                                } else {
                                    alpha = 255;
                                }

                                int argb = (alpha << 24) | (r << 16) | (g << 8) | b;
                                out.setRGB(x, y, argb);
                            }
                        }

                        File outFile = outputDir.resolve(
                                p.getFileName().toString().replaceAll("(?i)\\.jpg$", ".png")
                        ).toFile();

                        ImageIO.write(out, "png", outFile);
                        System.out.println("Wrote: " + outFile);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed on " + p + ": " + e.getMessage(), e);
                    }
                });
    }
}
