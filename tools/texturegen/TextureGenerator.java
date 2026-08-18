// ============================================================
// TCDEX Texture Generator (v2)
//
// 零依赖独立 Java 工具（JDK 11+ 单文件运行），生成：
//   1. 仿匠魂（Tinkers Construct 3）风格的熔融流体纹理
//   2. 原版 MC 像素画风格的材料物品图标（硬像素边缘，无抗锯齿）
//
// 流体风格参数来自对 TConstruct 3.11 熔融金属纹理的实测：
//   - still  = 16x16 帧垂直堆叠，10 级色相偏移调色板，
//              像素分布 中间调为主 / 暗结壳 ~7% / 亮热点 ~3%，
//              动画 = 每帧约 20% 像素在相邻色阶间翻动（"沸腾"）
//   - flow   = 32x32 帧，暗色结壳背景 + 向下移动的亮色流纹
//   - mcmeta = {"animation":{"frametime":2}}
// 调色板规律（实测铁 BDBDBD 灰度模板 → 690F0A..D0735F 等）：
//   暗端 → 亮端：亮度约 ×2、饱和度降 ~0.3、色相 +10° 偏移
//
// 物品风格参考原版 iron_ingot / gold_nugget / emerald 实测：
//   16x16 像素模板（字符网格），5 色调色板由单色自动派生
//   （轮廓/暗面/基色/亮面/高光，高光偏暖、轮廓偏冷）
//
// 输出（Forge 1.20.1 流体规范，路径与旧版一致）：
//   textures/block/<name>_still.png (+ .mcmeta)
//   textures/block/<name>_flow.png  (+ .mcmeta)
//   textures/item/<name>.png
//
// 用法（项目根目录运行）：
//   java tools/texturegen/TextureGenerator.java
//       读取 tools/texturegen/textures.txt，输出到
//       src/main/resources/assets/tcdex/textures
//   java tools/texturegen/TextureGenerator.java --fluid molten_prism A78BFA
//   java tools/texturegen/TextureGenerator.java --item prism_ingot A78BFA ingot
//   选项：
//     --frames N     流体动画帧数（默认 16；still 循环沸腾，flow 流纹正好下移一周）
//     --seed N       噪声种子（默认 0，同参数输出确定可复现）
//     --out DIR      输出目录（默认 src/main/resources/assets/tcdex/textures）
//     --config FILE  批量配置文件（默认 tools/texturegen/textures.txt）
//     --preview PNG  终端 ASCII 预览已生成的 PNG（动画显示前 4 帧）后退出
//
// textures.txt 格式（每行一条，'#' 注释）：
//   fluid:<流体名>,<十六进制颜色>[,帧数]
//   item:<物品名>,<十六进制颜色>[,形状]     形状: ingot|nugget|gem|plate（默认 ingot）
// ============================================================

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TextureGenerator {

    // ===== 常量（原版/匠魂规范，不建议改动） =====

    /** 流体静止帧边长（原版/匠魂均为 16） */
    private static final int STILL_SIZE = 16;
    /** 流体流动帧边长（原版水/岩浆、匠魂均为 32） */
    private static final int FLOW_SIZE = 32;
    /** mcmeta 帧间隔（匠魂熔融流体实测值） */
    private static final int FRAMETIME = 2;
    /** 流体调色板色阶数（匠魂实测 10 级） */
    private static final int LEVELS = 10;
    /** 物品图标边长 */
    private static final int ITEM_SIZE = 16;

    /**
     * still 像素亮度分布（实测匠魂熔融金属，10 级从暗到亮的累计占比）：
     * 暗结壳 ~7%、中间调为主、亮热点 ~3%。
     */
    private static final double[] STILL_CUM = {
            0.0078, 0.0654, 0.2324, 0.4453, 0.6318, 0.7725, 0.8877, 0.9668, 0.9971, 1.0
    };
    /** flow 背景分布（实测匠魂流动纹理：全色阶偏暗，8 级；亮流纹另行绘制到 8..9） */
    private static final double[] FLOW_BG_CUM = {
            0.032, 0.194, 0.414, 0.645, 0.824, 0.932, 0.983, 1.0
    };
    /** still 沸腾动画抖动幅度（归一化噪声单位；目标每帧约 20% 像素翻动，实测匠魂 51.8/256） */
    private static final double BOIL_JITTER = 0.070;
    /** 相干噪声权重（其余为逐像素颗粒）：相干噪声形成 2-4px 结壳 blob，颗粒打碎大色块 → 锯齿状边缘 */
    private static final double COHERENT_WEIGHT = 0.62;
    /** flow 流纹数量（32px 宽实测约 6 条） */
    private static final int STREAK_COUNT = 6;

    // ===== 选项 =====

    private static int frames = 16;
    private static long seed = 0;
    private static Path out = Paths.get("src/main/resources/assets/tcdex/textures");
    private static Path config = Paths.get("tools/texturegen/textures.txt");

    private static final class FluidSpec {
        final String name;
        final int color;
        final int frames;

        FluidSpec(String name, int color, int frames) {
            this.name = name;
            this.color = color;
            this.frames = Math.max(1, frames);
        }
    }

    private static final class ItemSpec {
        final String name;
        final int color;
        final String shape;

        ItemSpec(String name, int color, String shape) {
            this.name = name;
            this.color = color;
            this.shape = shape;
        }
    }

    // ============================================================
    // 物品像素模板（16x16 字符网格，参考原版 iron_ingot/gold_nugget/emerald 实测轮廓）
    //   O=轮廓(最暗)  D=暗面  B=基色  L=亮面  *=高光  空格=透明
    // ============================================================

    private static final Map<String, String[]> TEMPLATES = new LinkedHashMap<>();

    static {
        // 锭：仿原版 iron_ingot 斜向 3D 锭形（顶面亮、正面基色、底边暗、轮廓最暗）
        TEMPLATES.put("ingot", new String[]{
                "                ",
                "                ",
                "          DD    ",
                "       DDDBBD   ",
                "    DDDB**LLBD  ",
                " DDDB***LLLLLBD ",
                "D**LLLLLLLLLLLLD",
                "DBLLLLLLLLLLLDBO",
                "DBBLLLLLLLDDDDBO",
                "DBBBLLLDDDDDBBBO",
                "DDBBLDDDDDBBDOO ",
                " DDBLDDDDDOOO   ",
                "  DDBDDOOO      ",
                "   DDOO         ",
                "                ",
                "                ",
        });
        // 粒：仿原版 gold_nugget 圆块（亮顶 + 右侧暗边）
        TEMPLATES.put("nugget", new String[]{
                "                ",
                "                ",
                "                ",
                "                ",
                "      DDD       ",
                "     DL*LO      ",
                "     DL**LO     ",
                "     DLLLBO     ",
                "     DLLBBO     ",
                "      DLBBO     ",
                "      DLBO      ",
                "       DO       ",
                "                ",
                "                ",
                "                ",
                "                ",
        });
        // 宝石：仿原版 emerald 六边形切割（左上亮切面、中间基色、右下暗切面）
        TEMPLATES.put("gem", new String[]{
                "                ",
                "                ",
                "      OOOO      ",
                "     OL*BBO     ",
                "    OLLBBDBO    ",
                "   OLLLBBDDO    ",
                "   OLBLLDDDO    ",
                "   OLBLLDODO    ",
                "   OLBLLDODO    ",
                "   OLBLLDOBO    ",
                "   OLBBLOBODO   ",
                "    ODDOODLO    ",
                "     ODDOBO     ",
                "      OOOO      ",
                "                ",
                "                ",
        });
        // 板：平板（顶部亮倒角、右侧/底部暗阴影）
        TEMPLATES.put("plate", new String[]{
                "                ",
                "                ",
                "                ",
                "                ",
                "                ",
                "  OOOOOOOOOOOO  ",
                "  O**LLLLLLLLO  ",
                "  OLBBBBBBBDDO  ",
                "  OLBBBBBBBDDO  ",
                "  OLBBBBBBBDDO  ",
                "  ODDDDDDDDDDO  ",
                "  OOOOOOOOOOOO  ",
                "                ",
                "                ",
                "                ",
                "                ",
        });
    }

    /** 模板字符 → 调色板索引（O 轮廓 / D 暗面 / B 基色 / L 亮面 / * 高光） */
    private static int templateLevel(char c) {
        switch (c) {
            case 'O': return 0;
            case 'D': return 1;
            case 'B': return 2;
            case 'L': return 3;
            case '*': return 4;
            default: return -1; // 透明
        }
    }

    // ============================================================
    // main
    // ============================================================

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        validateTemplates();

        List<FluidSpec> fluids = new ArrayList<>();
        List<ItemSpec> items = new ArrayList<>();

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if ("--frames".equals(arg)) {
                frames = Math.max(1, Integer.parseInt(args[++i]));
            } else if ("--seed".equals(arg)) {
                seed = Long.parseLong(args[++i]);
            } else if ("--out".equals(arg)) {
                out = Paths.get(args[++i]);
            } else if ("--config".equals(arg)) {
                config = Paths.get(args[++i]);
            } else if ("--fluid".equals(arg)) {
                String name = args[++i];
                int color = parseColor(args[++i]);
                int f = frames;
                if (i + 1 < args.length && isNumber(args[i + 1])) {
                    f = Math.max(1, Integer.parseInt(args[++i]));
                }
                fluids.add(new FluidSpec(name, color, f));
            } else if ("--item".equals(arg)) {
                String name = args[++i];
                int color = parseColor(args[++i]);
                String shape = (i + 1 < args.length && TEMPLATES.containsKey(args[i + 1])) ? args[++i] : "ingot";
                items.add(new ItemSpec(name, color, shape));
            } else if ("--preview".equals(arg)) {
                preview(Paths.get(args[++i]));
                return;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
            i++;
        }

        // 批量配置（允许文件不存在）：命令行已给出条目时不再合并配置文件，避免重复
        if (fluids.isEmpty() && items.isEmpty() && Files.exists(config)) {
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String[] p = t.split(",");
                if (p.length < 2) {
                    System.out.println("  [skip] bad config line: " + t);
                    continue;
                }
                String head = p[0].trim();
                int color = parseColor(p[1].trim());
                if (head.startsWith("fluid:")) {
                    int f = p.length >= 3 ? Math.max(1, Integer.parseInt(p[2].trim())) : frames;
                    fluids.add(new FluidSpec(head.substring(6), color, f));
                } else if (head.startsWith("item:")) {
                    String shape = p.length >= 3 ? p[2].trim() : "ingot";
                    if (!TEMPLATES.containsKey(shape)) {
                        System.out.println("  [skip] unknown shape '" + shape + "' in line: " + t);
                        continue;
                    }
                    items.add(new ItemSpec(head.substring(5), color, shape));
                } else {
                    System.out.println("  [skip] entries must start with fluid: or item: -> " + t);
                }
            }
        }

        if (fluids.isEmpty() && items.isEmpty()) {
            System.out.println("Nothing to generate. Add entries to " + config
                    + " or pass --fluid/--item arguments.");
            return;
        }

        Files.createDirectories(out.resolve("block"));
        Files.createDirectories(out.resolve("item"));

        for (FluidSpec fluid : fluids) {
            writeFluid(fluid);
        }
        for (ItemSpec item : items) {
            writeItem(item);
        }

        System.out.println("Done: " + fluids.size() + " fluid(s) + " + items.size()
                + " item(s) -> " + out.toAbsolutePath());
    }

    private static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ============================================================
    // 流体（仿匠魂熔融金属）
    // ============================================================

    private static void writeFluid(FluidSpec spec) throws IOException {
        int[] ramp = fluidRamp(spec.color);
        long fluidSeed = ((long) spec.color << 16) ^ (seed * 0x9E3779B9L);

        BufferedImage still = stack(renderStillFrames(spec, ramp, fluidSeed));
        BufferedImage flow = stack(renderFlowFrames(spec, ramp, fluidSeed));

        Path stillPath = out.resolve("block/" + spec.name + "_still.png");
        Path flowPath = out.resolve("block/" + spec.name + "_flow.png");
        ImageIO.write(still, "png", stillPath.toFile());
        ImageIO.write(flow, "png", flowPath.toFile());
        writeAnimationMeta(stillPath);
        writeAnimationMeta(flowPath);

        System.out.println("  fluid  " + spec.name + " (" + Integer.toHexString(spec.color)
                + ", " + spec.frames + " frame(s), still " + still.getWidth() + "x" + still.getHeight()
                + ", flow " + flow.getWidth() + "x" + flow.getHeight() + ")");
    }

    /** still 帧序列：同一噪声底图 + 周期性沸腾抖动（正弦相位 → 无缝循环） */
    private static BufferedImage[] renderStillFrames(FluidSpec spec, int[] ramp, long fluidSeed) {
        double[][] base = noiseField(STILL_SIZE, fluidSeed);
        double[][] grain = phaseField(STILL_SIZE, STILL_SIZE, fluidSeed ^ 0x3C6EL);
        double[][] phase = phaseField(STILL_SIZE, STILL_SIZE, fluidSeed ^ 0x51A7L);
        int[] counts = levelCounts(STILL_CUM, STILL_SIZE * STILL_SIZE);

        BufferedImage[] result = new BufferedImage[spec.frames];
        for (int f = 0; f < spec.frames; f++) {
            double[] vals = new double[STILL_SIZE * STILL_SIZE];
            for (int y = 0; y < STILL_SIZE; y++) {
                for (int x = 0; x < STILL_SIZE; x++) {
                    double boil = BOIL_JITTER * Math.sin(2.0 * Math.PI * f / spec.frames + phase[y][x] * 2.0 * Math.PI);
                    vals[y * STILL_SIZE + x] = mix(base[y][x], grain[y][x]) + boil;
                }
            }
            int[] levels = rankLevels(vals, counts);
            BufferedImage img = new BufferedImage(STILL_SIZE, STILL_SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < STILL_SIZE; y++) {
                for (int x = 0; x < STILL_SIZE; x++) {
                    img.setRGB(x, y, 0xFF000000 | ramp[levels[y * STILL_SIZE + x]]);
                }
            }
            result[f] = img;
        }
        return result;
    }

    /**
     * flow 帧序列：整幅暗色结壳噪声向下滚动（匠魂流动纹理的真实动画方式）+ 亮色流纹点缀。
     * 滚动速度 = 32/帧数 px/帧，帧数滚动满一周 → 无缝循环。
     */
    private static BufferedImage[] renderFlowFrames(FluidSpec spec, int[] ramp, long fluidSeed) {
        double[][] bgNoise = noiseField(FLOW_SIZE, fluidSeed ^ 0xF10E1L);
        double[][] bgGrain = phaseField(FLOW_SIZE, FLOW_SIZE, fluidSeed ^ 0x29A3L);
        int[] bgCounts = levelCounts(FLOW_BG_CUM, FLOW_SIZE * FLOW_SIZE);

        BufferedImage[] result = new BufferedImage[spec.frames];
        for (int f = 0; f < spec.frames; f++) {
            int shift = (int) Math.round(f * (double) FLOW_SIZE / spec.frames);
            // 噪声按行滚动采样（纹理可平铺，取模即无缝）；值的多重集不变 → 分位数色阶随之平移
            double[] vals = new double[FLOW_SIZE * FLOW_SIZE];
            for (int y = 0; y < FLOW_SIZE; y++) {
                int yy = (y + shift) % FLOW_SIZE;
                for (int x = 0; x < FLOW_SIZE; x++) {
                    vals[y * FLOW_SIZE + x] = mix(bgNoise[yy][x], bgGrain[yy][x]);
                }
            }
            int[] bgLevels = rankLevels(vals, bgCounts);
            BufferedImage img = new BufferedImage(FLOW_SIZE, FLOW_SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < FLOW_SIZE; y++) {
                for (int x = 0; x < FLOW_SIZE; x++) {
                    img.setRGB(x, y, 0xFF000000 | ramp[bgLevels[y * FLOW_SIZE + x]]);
                }
            }
            drawStreaks(img, shift, ramp, fluidSeed);
            result[f] = img;
        }
        return result;
    }

    /** 亮色流纹：确定性位置的横向短划线，随滚动同步下移（无缝循环），两端降 2 级色阶收边 */
    private static void drawStreaks(BufferedImage img, int shift, int[] ramp, long fluidSeed) {
        for (int k = 0; k < STREAK_COUNT; k++) {
            int x = 1 + (int) (hash(k, 11, fluidSeed) % (FLOW_SIZE - 7));
            int len = 2 + (int) (hash(k, 22, fluidSeed) % 3);          // 2..4 px 宽
            int lvl = 8 + (int) (hash(k, 33, fluidSeed) % 2);          // 色阶 8..9（比背景亮）
            int y = (int) ((hash(k, 44, fluidSeed) + shift) % FLOW_SIZE);
            setPx(img, x - 1, y, ramp[lvl - 2]);
            for (int dx = 0; dx < len; dx++) {
                setPx(img, x + dx, y, ramp[lvl]);
            }
            setPx(img, x + len, y, ramp[lvl - 2]);
        }
    }

    private static void setPx(BufferedImage img, int x, int y, int rgb) {
        if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
            img.setRGB(x, y, 0xFF000000 | rgb);
        }
    }

    /**
     * 流体 10 级调色板（实测规律：暗端→亮端 亮度 ×~2、饱和度降 ~0.3、色相 +10° 偏移）。
     * 输入色约对应色阶中段。
     */
    private static int[] fluidRamp(int color) {
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        int[] ramp = new int[LEVELS];
        for (int lvl = 0; lvl < LEVELS; lvl++) {
            double t = lvl / (double) (LEVELS - 1);
            float h = wrapHue(hsb[0] + (float) (-6.0 + 18.0 * t) / 360.0);  // -6° → +12°
            float s = clamp01(hsb[1] * (float) (1.10 - 0.40 * t));          // 饱和度 1.10 → 0.70
            float b = clamp01(hsb[2] * (float) (0.55 + 0.80 * t));          // 亮度 0.55 → 1.35
            ramp[lvl] = Color.HSBtoRGB(h, s, b) & 0xFFFFFF;
        }
        return ramp;
    }

    private static void writeAnimationMeta(Path png) throws IOException {
        Path meta = png.resolveSibling(png.getFileName() + ".mcmeta");
        Files.write(meta,
                ("{\n  \"animation\": {\n    \"frametime\": " + FRAMETIME + "\n  }\n}\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    // ============================================================
    // 物品（原版像素画风格）
    // ============================================================

    private static void writeItem(ItemSpec spec) throws IOException {
        String[] template = TEMPLATES.get(spec.shape);
        int[] palette = itemPalette(spec.color);
        BufferedImage img = new BufferedImage(ITEM_SIZE, ITEM_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < ITEM_SIZE; y++) {
            for (int x = 0; x < ITEM_SIZE; x++) {
                int level = templateLevel(template[y].charAt(x));
                if (level >= 0) {
                    img.setRGB(x, y, 0xFF000000 | palette[level]);
                }
            }
        }
        Path path = out.resolve("item/" + spec.name + ".png");
        ImageIO.write(img, "png", path.toFile());
        System.out.println("  item   " + spec.name + " (" + Integer.toHexString(spec.color)
                + ", " + spec.shape + ")");
    }

    /**
     * 物品 5 色调色板（原版像素画规律：轮廓最暗偏冷、高光偏暖近白）。
     * 返回 [轮廓, 暗面, 基色, 亮面, 高光]。
     */
    private static int[] itemPalette(int color) {
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        float h = hsb[0], s = hsb[1], b = hsb[2];
        return new int[]{
                Color.HSBtoRGB(wrapHue(h + 8f / 360f), clamp01(s * 1.15f), clamp01(b * 0.22f)),  // 轮廓
                Color.HSBtoRGB(wrapHue(h + 4f / 360f), clamp01(s * 1.05f), clamp01(b * 0.55f)),  // 暗面
                Color.HSBtoRGB(wrapHue(h), clamp01(s), clamp01(b)),                              // 基色
                Color.HSBtoRGB(wrapHue(h - 6f / 360f), clamp01(s * 0.85f), clamp01(b * 1.35f)),  // 亮面
                Color.HSBtoRGB(wrapHue(h - 14f / 360f), clamp01(s * 0.45f), clamp01(b * 1.7f + 0.12f)), // 高光
        };
    }

    /** 模板自检：必须 16 行 × 16 字符，字符合法（启动即校验，防止手误） */
    private static void validateTemplates() {
        for (Map.Entry<String, String[]> entry : TEMPLATES.entrySet()) {
            String[] rows = entry.getValue();
            if (rows.length != ITEM_SIZE) {
                throw new IllegalStateException("template '" + entry.getKey() + "' must have 16 rows, got " + rows.length);
            }
            for (int y = 0; y < ITEM_SIZE; y++) {
                if (rows[y].length() != ITEM_SIZE) {
                    throw new IllegalStateException("template '" + entry.getKey() + "' row " + y
                            + " must be 16 chars, got " + rows[y].length() + ": '" + rows[y] + "'");
                }
                for (int x = 0; x < ITEM_SIZE; x++) {
                    char c = rows[y].charAt(x);
                    if (templateLevel(c) < 0 && c != ' ') {
                        throw new IllegalStateException("template '" + entry.getKey() + "' row " + y
                                + " has invalid char '" + c + "'");
                    }
                }
            }
        }
    }

    // ============================================================
    // 噪声 / 分位数色阶
    // ============================================================

    /** 可平铺值噪声场（纹理在游戏内重复拼接，边缘必须无缝）：粗网格 blob + 细网格细节 */
    private static double[][] noiseField(int size, long noiseSeed) {
        double[][] field = new double[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double coarse = tiledNoise(x / 4.0, y / 4.0, size / 4, noiseSeed);
                double fine = tiledNoise(x / 2.0, y / 2.0, size / 2, noiseSeed ^ 0x7F4A7C15L);
                field[y][x] = coarse * 0.72 + fine * 0.28;
            }
        }
        return field;
    }

    /** 每像素随机相位（沸腾动画用），[0,1) */
    private static double[][] phaseField(int w, int h, long phaseSeed) {
        double[][] field = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                field[y][x] = hash01(x, y, phaseSeed);
            }
        }
        return field;
    }

    /** 周期平滑值噪声：格点按 period 取模 → 输出纹理可无缝平铺 */
    private static double tiledNoise(double x, double y, int period, long noiseSeed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double xf = x - xi, yf = y - yi;
        double u = xf * xf * (3 - 2 * xf);
        double v = yf * yf * (3 - 2 * yf);
        double a = hash01(wrap(xi, period), wrap(yi, period), noiseSeed);
        double b = hash01(wrap(xi + 1, period), wrap(yi, period), noiseSeed);
        double c = hash01(wrap(xi, period), wrap(yi + 1, period), noiseSeed);
        double d = hash01(wrap(xi + 1, period), wrap(yi + 1, period), noiseSeed);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static int wrap(int value, int period) {
        return ((value % period) + period) % period;
    }

    /** 整数哈希 → [0,1) */
    private static double hash01(int x, int y, long hseed) {
        return hash(x, y, hseed) / (double) Long.MAX_VALUE;
    }

    /** 确定性整数哈希（种子混合，输出均匀分布） */
    private static long hash(int x, int y, long hseed) {
        long h = (long) x * 374761393L + (long) y * 668265263L + hseed * 1442695041L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return (h ^ (h >>> 16)) & Long.MAX_VALUE;
    }

    /** 按累计分布把总像素数切分为各色阶的数量（保证总和精确） */
    private static int[] levelCounts(double[] cumulative, int total) {
        int[] counts = new int[cumulative.length];
        int prev = 0;
        for (int lvl = 0; lvl < cumulative.length; lvl++) {
            int upto = (int) Math.round(cumulative[lvl] * total);
            counts[lvl] = upto - prev;
            prev = upto;
        }
        counts[counts.length - 1] += total - prev;
        return counts;
    }

    /**
     * 按值排序分配色阶：噪声值最小的像素拿最低色阶，依此类推。
     * 保证每帧像素分布与目标分布精确一致（空间连续性由噪声本身提供 → blob 形状）。
     */
    private static int[] rankLevels(double[] values, int[] counts) {
        Integer[] order = new Integer[values.length];
        for (int idx = 0; idx < values.length; idx++) {
            order[idx] = idx;
        }
        Arrays.sort(order, (a, b) -> Double.compare(values[a], values[b]));
        int[] levels = new int[values.length];
        int cursor = 0;
        for (int lvl = 0; lvl < counts.length; lvl++) {
            for (int k = 0; k < counts[lvl]; k++) {
                levels[order[cursor++]] = lvl;
            }
        }
        return levels;
    }

    /** 相干噪声 + 逐像素颗粒混合（颗粒打碎大色块 → 匠魂风格锯齿状结壳边缘） */
    private static double mix(double coherent, double grainValue) {
        return COHERENT_WEIGHT * coherent + (1.0 - COHERENT_WEIGHT) * grainValue;
    }

    // ============================================================
    // 通用
    // ============================================================

    /** 帧垂直堆叠 → 单张动画精灵图（MC 流体动画规范） */
    private static BufferedImage stack(BufferedImage[] framesToStack) {
        int w = framesToStack[0].getWidth();
        int h = framesToStack[0].getHeight();
        BufferedImage stacked = new BufferedImage(w, h * framesToStack.length, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = stacked.createGraphics();
        for (int idx = 0; idx < framesToStack.length; idx++) {
            g.drawImage(framesToStack[idx], 0, idx * h, null);
        }
        g.dispose();
        return stacked;
    }

    private static int parseColor(String s) {
        String hex = s.replace("#", "").trim();
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Color must be 6 hex digits, got: " + s);
        }
        return Integer.parseInt(hex, 16);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static float wrapHue(double h) {
        return (float) (((h % 1.0) + 1.0) % 1.0);
    }

    /** 终端 ASCII 预览：动画图显示前 4 帧，静态图直接显示 */
    private static void preview(Path png) throws IOException {
        BufferedImage img = ImageIO.read(png.toFile());
        if (img == null) {
            throw new IOException("Cannot read PNG: " + png);
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int frameCount = h / w;
        int show = Math.min(4, frameCount);
        System.out.println("Preview: " + png + " (" + w + "x" + h + ", " + frameCount + " frame(s))");
        for (int f = 0; f < show; f++) {
            System.out.println("--- frame " + f + " ---");
            for (int y = 0; y < w; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, f * w + y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                    double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                    sb.append(a < 32 ? ' ' : " .:-=+*#%@".charAt(Math.min(9, (int) (lum * 10))));
                }
                System.out.println(sb);
            }
        }
    }
}
