package com.vladik.godbox.client;

import com.vladik.godbox.net.SeasonPayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * v3.2: сезон на клиенте. Красит траву, листву и воду по сезонам (хуки в mixin/client) и
 * постепенно перестраивает чанки вокруг игрока при смене цвета (4 шага за сезон).
 * Без пакета от сервера (чужой сервер, обычная игра) окраска выключена.
 */
public final class SeasonClient {
    private record Prof(int rgb, float k) {}

    private static final Prof[] GRASS = {new Prof(0x86D04A, 0.30f), new Prof(0x3E8F2A, 0.25f), new Prof(0xB4A030, 0.55f), new Prof(0xC9D5C3, 0.60f)};
    private static final Prof[] FOLIAGE = {new Prof(0x7ED957, 0.25f), new Prof(0x2E7D1E, 0.30f), new Prof(0xE0701A, 0.75f), new Prof(0x9AA68E, 0.60f)};
    private static final Prof[] BIRCH = {new Prof(0x9FD65F, 0.25f), new Prof(0x5E9440, 0.20f), new Prof(0xF0C628, 0.85f), new Prof(0xC9CFBF, 0.60f)};
    private static final Prof[] SPRUCE = {new Prof(0x619961, 0f), new Prof(0x619961, 0f), new Prof(0x4E7A4E, 0.20f), new Prof(0xB5C8BE, 0.45f)};
    private static final Prof[] WATER = {new Prof(0x3F76E4, 0f), new Prof(0x3F76E4, 0f), new Prof(0x3F76E4, 0f), new Prof(0x8FB5D8, 0.25f)};

    public static volatile boolean on, tint;
    public static volatile int season, year = 1, length = 6000;
    public static volatile float progress;
    public static volatile String effects = "";
    public static volatile long updated;

    // квантованная фаза: с запасом на середины сезонов; меняется 4 раза за сезон
    private static volatile int a;
    private static volatile float f;
    private static int appliedKey = -1;
    private static final List<int[]> ORDER = new ArrayList<>();
    private static int orderR = -1, cursor, ox, oz;
    private static boolean rebuilding;

    private SeasonClient() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SeasonPayloads.Sync.ID, (p, ctx) -> ctx.client().execute(() -> set(p)));
        ClientTickEvents.END_CLIENT_TICK.register(SeasonClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());
    }

    private static void reset() {
        on = false;
        tint = false;
        effects = "";
        updated = 0;
        appliedKey = -1;
        rebuilding = false;
    }

    private static void set(SeasonPayloads.Sync p) {
        on = p.on();
        tint = p.tint();
        season = Math.floorMod(p.season(), 4);
        year = p.year();
        length = Math.max(200, p.length());
        progress = Math.max(0f, Math.min(0.999f, p.progress() / 1000f));
        effects = p.effects();
        updated = System.currentTimeMillis();
        float q = (float) ((Math.floor((season + progress) * 4.0) + 0.5) / 4.0) - 0.5f;
        int fa = (int) Math.floor(q);
        a = Math.floorMod(fa, 4);
        f = q - fa;
    }

    public static boolean tintActive() { return on && tint; }

    // ---------------- окраска ----------------
    private static float scale(float temp) { return temp >= 1.5f ? 0.2f : temp >= 0.95f ? 0.6f : 1f; }

    private static int mix(int c, int rgb, float t) {
        int r = (int) (((c >> 16) & 255) * (1 - t) + ((rgb >> 16) & 255) * t);
        int g = (int) (((c >> 8) & 255) * (1 - t) + ((rgb >> 8) & 255) * t);
        int b = (int) ((c & 255) * (1 - t) + (rgb & 255) * t);
        return (c & 0xFF000000) | r << 16 | g << 8 | b;
    }

    private static int blend(int c, Prof[] p, float scale) {
        Prof x = p[a], y = p[(a + 1) & 3];
        if (x.k * scale == 0 && y.k * scale == 0) return c;
        int c1 = mix(c, x.rgb, x.k * scale), c2 = mix(c, y.rgb, y.k * scale);
        return mix(c1, c2 & 0xFFFFFF, f) & 0xFFFFFF | (c & 0xFF000000);
    }

    public static int grass(int c, float temp) { return blend(c, GRASS, scale(temp)); }

    public static int foliage(int c, float temp) { return blend(c, FOLIAGE, scale(temp)); }

    public static int birch(int c) { return blend(c, BIRCH, 1f); }

    public static int spruce(int c) { return blend(c, SPRUCE, 1f); }

    public static int water(int c, float temp) { return blend(c, WATER, scale(temp)); }

    // ---------------- перестроение чанков ----------------
    private static void tick(MinecraftClient mc) {
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) return;
        int key = tintActive() ? season * 4 + (int) (progress * 4) : -1;
        if (key != appliedKey) {
            boolean first = appliedKey == -1 && key == -1;
            appliedKey = key;
            if (!first) {
                w.reloadColor();
                int r = Math.max(2, mc.options.getClampedViewDistance());
                if (orderR != r) {
                    orderR = r;
                    ORDER.clear();
                    for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) if (x * x + z * z <= r * r + r) ORDER.add(new int[]{x, z});
                    ORDER.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
                }
                cursor = 0;
                ox = mc.player.getChunkPos().x;
                oz = mc.player.getChunkPos().z;
                rebuilding = true;
            }
        }
        if (!rebuilding) return;
        int bottom = w.getBottomSectionCoord(), top = w.getTopSectionCoord();
        for (int n = 0; n < 6 && cursor < ORDER.size(); n++) {
            int[] o = ORDER.get(cursor++);
            int cx = ox + o[0], cz = oz + o[1];
            if (w.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false) == null) continue;
            for (int sy = bottom; sy < top; sy++) mc.worldRenderer.scheduleBlockRender(cx, sy, cz);
        }
        if (cursor >= ORDER.size()) rebuilding = false;
    }
}
