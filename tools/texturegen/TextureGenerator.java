// ============================================================
// TCDEX Texture Generator - standalone Java texture generator
// for fluids & material item icons (no external dependencies).
//
// Forge 1.20.1 fluid texture conventions:
//   textures/block/<name>_still.png   - still frame(s), N frames stacked
//                                       vertically when --frames > 1
//   textures/block/<name>_still.png.mcmeta - animation metadata (frametime 2)
//   textures/block/<name>_flow.png    - flowing frame(s) + .mcmeta
//   textures/item/<name>.png          - ingot icon (16x16)
//
// Usage (run from project root, JDK 11+ single-file mode):
//   java tools/texturegen/TextureGenerator.java
//       reads tools/texturegen/textures.txt and generates into
//       src/main/resources/assets/tcdex/textures
//   java tools/texturegen/TextureGenerator.java --fluid molten_tcdexium FF8844 molten
//   java tools/texturegen/TextureGenerator.java --item tcdexium_ingot FF8844
//   java tools/texturegen/TextureGenerator.java --size 32 --frames 4 --out <dir> --config <file>
//
// textures.txt format (one entry per line, '#' = comment):
//   <fluid_name>,<hex_color>[,style]      style: molten|water|simple (default molten)
//   item:<item_name>,<hex_color>          -> 16x16 ingot icon
// ============================================================

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TextureGenerator {

    private static int SIZE = 32;          // fluid frame size (square)
    private static int FRAMES = 4;         // animation frames (1 = static)
    private static int FRAMETIME = 2;      // mcmeta frametime
    private static Path OUT = Paths.get("src/main/resources/assets/tcdex/textures");
    private static Path CONFIG = Paths.get("tools/texturegen/textures.txt");

    private static final class FluidSpec {
        final String name;
        final int color;
        final String style;

        FluidSpec(String name, int color, String style) {
            this.name = name;
            this.color = color;
            this.style = style;
        }
    }

    private static final class ItemSpec {
        final String name;
        final int color;

        ItemSpec(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        List<FluidSpec> fluids = new ArrayList<>();
        List<ItemSpec> items = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size" -> SIZE = Integer.parseInt(args[++i]);
                case "--frames" -> FRAMES = Math.max(1, Integer.parseInt(args[++i]));
                case "--out" -> OUT = Paths.get(args[++i]);
                case "--config" -> CONFIG = Paths.get(args[++i]);
                case "--fluid" -> {
                    String name = args[++i];
                    int color = parseColor(args[++i]);
                    String style = (i + 1 < args.length && isStyle(args[i + 1])) ? args[++i] : "molten";
                    fluids.add(new FluidSpec(name, color, style));
                }
                case "--item" -> items.add(new ItemSpec(args[++i], parseColor(args[++i])));
                case "--preview" -> {
                    // terminal ASCII preview of a generated PNG (brightness map)
                    BufferedImage img = ImageIO.read(Paths.get(args[++i]).toFile());
                    if (img == null) {
                        throw new IOException("Cannot read PNG: " + args[i]);
                    }
                    System.out.println("Preview: " + args[i] + " (" + img.getWidth() + "x" + img.getHeight() + ")");
                    int cols = 48;
                    int rows = Math.max(8, img.getHeight() * cols / img.getWidth() / 2);
                    for (int y = 0; y < rows; y++) {
                        StringBuilder sb = new StringBuilder();
                        for (int x = 0; x < cols; x++) {
                            int px = Math.min(img.getWidth() - 1, x * img.getWidth() / cols);
                            int py = Math.min(img.getHeight() - 1, y * img.getHeight() / rows);
                            int argb = img.getRGB(px, py);
                            int a = (argb >>> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                            double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                            sb.append(a < 32 ? ' ' : " .:-=+*#%@".charAt(Math.min(9, (int) (lum * 10))));
                        }
                        System.out.println(sb);
                    }
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }

        // batch config file (tolerate missing file)
        if (Files.exists(CONFIG)) {
            for (String line : Files.readAllLines(CONFIG, StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String[] p = t.split(",");
                if (p.length < 2) {
                    continue;
                }
                String name = p[0].trim();
                int color = parseColor(p[1].trim());
                if (name.startsWith("item:")) {
                    items.add(new ItemSpec(name.substring(5), color));
                } else {
                    String style = p.length >= 3 ? p[2].trim() : "molten";
                    fluids.add(new FluidSpec(name, color, style));
                }
            }
        }

        if (fluids.isEmpty() && items.isEmpty()) {
            System.out.println("Nothing to generate. Add entries to " + CONFIG
                    + " or pass --fluid/--item arguments.");
            return;
        }

        Files.createDirectories(OUT.resolve("block"));
        Files.createDirectories(OUT.resolve("item"));

        for (FluidSpec fluid : fluids) {
            writeFluid(fluid);
        }
        for (ItemSpec item : items) {
            writeIngot(item);
        }

        System.out.println("Done: " + fluids.size() + " fluid(s) + " + items.size()
                + " item(s) -> " + OUT.toAbsolutePath());
    }

    // ============================================================
    // fluids
    // ============================================================

    private static void writeFluid(FluidSpec spec) throws IOException {
        Path stillPath = OUT.resolve("block/" + spec.name + "_still.png");
        Path flowPath = OUT.resolve("block/" + spec.name + "_flow.png");
        if (FRAMES > 1) {
            ImageIO.write(stackFrames(renderFrames(spec, false)), "png", stillPath.toFile());
            ImageIO.write(stackFrames(renderFrames(spec, true)), "png", flowPath.toFile());
            writeAnimationMeta(stillPath);
            writeAnimationMeta(flowPath);
        } else {
            ImageIO.write(renderFrame(spec, false, 0, 1), "png", stillPath.toFile());
            ImageIO.write(renderFrame(spec, true, 0, 1), "png", flowPath.toFile());
        }
        System.out.println("  fluid  " + spec.name + " (" + Integer.toHexString(spec.color)
                + ", " + spec.style + ", " + FRAMES + " frame(s))");
    }

    private static BufferedImage[] renderFrames(FluidSpec spec, boolean flow) {
        BufferedImage[] frames = new BufferedImage[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            frames[i] = renderFrame(spec, flow, i, FRAMES);
        }
        return frames;
    }

    /** vertical stack of frames -> single animation sprite */
    private static BufferedImage stackFrames(BufferedImage[] frames) {
        int w = frames[0].getWidth();
        int h = frames[0].getHeight() * frames.length;
        BufferedImage stacked = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = stacked.createGraphics();
        for (int i = 0; i < frames.length; i++) {
            g.drawImage(frames[i], 0, i * frames[0].getHeight(), null);
        }
        g.dispose();
        return stacked;
    }

    private static BufferedImage renderFrame(FluidSpec spec, boolean flow, int frame, int frames) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int r = (spec.color >> 16) & 0xFF;
        int g = (spec.color >> 8) & 0xFF;
        int b = spec.color & 0xFF;
        double phase = frames > 1 ? 2.0 * Math.PI * frame / frames : 0.0;
        // deterministic per-frame noise
        Random rand = new Random(spec.color * 2654435761L + frame * 40503L + (flow ? 1 : 0));

        for (int y = 0; y < SIZE; y++) {
            double rowPhase = phase + y * 0.55; // flow: each row shifted -> horizontal motion
            for (int x = 0; x < SIZE; x++) {
                double v;
                if ("molten".equals(spec.style)) {
                    double wave = Math.sin((x + y * 0.6) * 0.35 + (flow ? rowPhase : phase) * 2.2);
                    double glow = 1.0 - (double) y / SIZE;      // bright molten top
                    double dim = (double) y / SIZE * 0.55;      // darker bottom
                    v = 0.58 + 0.24 * wave + 0.32 * glow - 0.55 * dim;
                    if (rand.nextDouble() < 0.018) {
                        v += 0.45;                              // hot sparks
                    }
                } else if ("water".equals(spec.style)) {
                    double wave = Math.sin((x + y * 0.3) * 0.25 + (flow ? rowPhase : phase) * 1.5);
                    double depth = (double) y / SIZE;
                    v = 0.82 - 0.38 * depth + 0.10 * wave;
                } else { // simple
                    v = 0.94 + rand.nextDouble() * 0.06;
                }
                v = Math.max(0.0, Math.min(1.0, v));
                img.setRGB(x, y, 0xFF000000 | shade(r, g, b, v));
            }
        }
        return img;
    }

    private static void writeAnimationMeta(Path png) throws IOException {
        Path meta = png.resolveSibling(png.getFileName() + ".mcmeta");
        Files.writeString(meta,
                "{\n  \"animation\": {\n    \"frametime\": " + FRAMETIME + "\n  }\n}\n",
                StandardCharsets.UTF_8);
    }

    // ============================================================
    // material item icons (ingot)
    // ============================================================

    private static void writeIngot(ItemSpec spec) throws IOException {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int r = (spec.color >> 16) & 0xFF;
        int g2 = (spec.color >> 8) & 0xFF;
        int b = spec.color & 0xFF;

        // ingot silhouette: tapered top, flat bottom
        Polygon body = new Polygon();
        body.addPoint(4, 3);
        body.addPoint(12, 3);
        body.addPoint(14, 6);
        body.addPoint(14, 13);
        body.addPoint(12, 14);
        body.addPoint(4, 14);
        body.addPoint(2, 13);
        body.addPoint(2, 6);

        // vertical gradient: bright top edge -> base -> darker bottom
        for (int y = 3; y <= 14; y++) {
            double t = (y - 3) / 11.0;
            double bright = 1.0 - t * 0.55;
            g.setColor(new Color(shade(r, g2, b, bright)));
            // fill row inside polygon manually for a clean gradient
            for (int x = 0; x < size; x++) {
                if (body.contains(x + 0.5, y + 0.5)) {
                    img.setRGB(x, y, 0xFF000000 | shade(r, g2, b, bright));
                }
            }
        }

        // top highlight line
        g.setColor(new Color(shade(r, g2, b, 1.25)));
        g.drawLine(5, 3, 11, 3);
        g.drawLine(3, 5, 5, 3);
        g.drawLine(13, 5, 11, 3);

        // dark outline
        g.setColor(new Color(shade(r, g2, b, 0.55)));
        g.draw(body);

        g.dispose();
        Path path = OUT.resolve("item/" + spec.name + ".png");
        ImageIO.write(img, "png", path.toFile());
        System.out.println("  item   " + spec.name + " (" + Integer.toHexString(spec.color) + ")");
    }

    // ============================================================
    // helpers
    // ============================================================

    private static int parseColor(String s) {
        String hex = s.replace("#", "").trim();
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Color must be 6 hex digits, got: " + s);
        }
        return Integer.parseInt(hex, 16);
    }

    private static boolean isStyle(String s) {
        return "molten".equals(s) || "water".equals(s) || "simple".equals(s);
    }

    /** scale rgb by brightness v (0..1.3), clamped */
    private static int shade(int r, int g, int b, double v) {
        int rr = (int) Math.min(255, r * v);
        int gg = (int) Math.min(255, g * v);
        int bb = (int) Math.min(255, b * v);
        return (rr << 16) | (gg << 8) | bb;
    }
}
