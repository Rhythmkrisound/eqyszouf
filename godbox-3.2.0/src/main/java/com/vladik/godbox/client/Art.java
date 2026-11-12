package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** v3.2: текстурные рамки, кнопки, значки и гербы (атлас textures/gui/ui.png, генератор tools/ui/gen.py). */
public final class Art {
    static final Identifier TEX = Identifier.of("godbox", "textures/gui/ui.png");
    private static final int T = 256;

    // области атласа (x, y, w, h, рамка) — совпадают с tools/ui/gen.py
    private static final int[] PANEL = {0, 0, 24, 24, 6};
    private static final int[] INSET = {24, 0, 16, 16, 3};
    private static final int[] TIP = {40, 0, 16, 16, 4};
    private static final int[] TRACK = {56, 0, 6, 16, 2};
    private static final int[] THUMB = {62, 0, 6, 16, 2};
    private static final int[] RIBBON = {72, 0, 32, 16, 8};
    private static final int[][] BTN = {{0, 24, 16, 16, 3}, {16, 24, 16, 16, 3}, {32, 24, 16, 16, 3}, {48, 24, 16, 16, 3}};
    private static final int[] TAB_ON = {64, 24, 16, 16, 3};
    private static final int[] TAB_OFF = {80, 24, 16, 16, 3};
    private static final int[] SLOT_SEL = {96, 24, 16, 16, 3};
    private static final int[] SLOT_HOT = {112, 24, 16, 16, 3};

    private static final int[] THIN = {128, 24, 12, 12, 3};

    public static final int CLOSE = 0, SEARCH = 1, STAR = 2, STAR_OFF = 3, LEFT = 4, RIGHT = 5, UP = 6, DOWN = 7, PIN = 8, CHECK = 9, PLUS = 10;

    private static boolean failed;
    private static boolean checked;
    private static boolean enabled = true;

    private Art() {}

    /** Переключатель «плоского» режима (ui.art). */
    static void setEnabled(boolean on) {
        enabled = on;
    }

    /** Текстуры используются, если включены и атлас найден. */
    static boolean on() {
        if (!enabled) return false;
        if (!checked) {
            checked = true;
            try {
                failed = MinecraftClient.getInstance().getResourceManager().getResource(TEX).isEmpty();
            } catch (Throwable t) {
                failed = true;
            }
        }
        return !failed;
    }

    private static void nine(DrawContext ctx, int[] r, int x, int y, int w, int h) {
        int b = r[4];
        if (w < 2 * b || h < 2 * b) b = Math.max(1, Math.min(w, h) / 2);
        int rb = r[4];
        int cw = r[2] - 2 * rb, ch = r[3] - 2 * rb;
        int mw = w - 2 * b, mh = h - 2 * b;
        int u = r[0], v = r[1];
        int rx = u + r[2] - rb, ry = v + r[3] - rb;
        ctx.drawTexture(TEX, x, y, b, b, u, v, rb, rb, T, T);
        ctx.drawTexture(TEX, x + w - b, y, b, b, rx, v, rb, rb, T, T);
        ctx.drawTexture(TEX, x, y + h - b, b, b, u, ry, rb, rb, T, T);
        ctx.drawTexture(TEX, x + w - b, y + h - b, b, b, rx, ry, rb, rb, T, T);
        if (mw > 0) {
            ctx.drawTexture(TEX, x + b, y, mw, b, u + rb, v, cw, rb, T, T);
            ctx.drawTexture(TEX, x + b, y + h - b, mw, b, u + rb, ry, cw, rb, T, T);
        }
        if (mh > 0) {
            ctx.drawTexture(TEX, x, y + b, b, mh, u, v + rb, rb, ch, T, T);
            ctx.drawTexture(TEX, x + w - b, y + b, b, mh, rx, v + rb, rb, ch, T, T);
        }
        if (mw > 0 && mh > 0) ctx.drawTexture(TEX, x + b, y + b, mw, mh, u + rb, v + rb, cw, ch, T, T);
    }

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, PANEL, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.BG);
    }

    /** Тонкая рамка (3 px) для плотных панелей, где содержимое подходит к краю. */
    static void frame(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, THIN, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.BG);
    }

    /** Тёмная вставка под слоты и списки. */
    static void inset(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, INSET, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.CELL);
    }

    static void tooltip(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, TIP, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.BG_SOLID);
    }

    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean active) {
        if (on()) nine(ctx, BTN[active ? 2 : hovered ? 1 : 0], x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, active ? Ui.CELL_SEL : hovered ? Ui.CELL_HOVER : Ui.CELL);
    }

    static void buttonOff(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, BTN[3], x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.CELL);
    }

    static void tab(DrawContext ctx, int x, int y, int w, int h, boolean selected, boolean hovered) {
        if (on()) nine(ctx, selected ? TAB_ON : hovered ? BTN[1] : TAB_OFF, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, selected ? Ui.CELL_SEL : hovered ? Ui.CELL_HOVER : Ui.CELL);
    }

    /** Рамка поверх выбранного слота (центр прозрачный). */
    static void slotSelected(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, SLOT_SEL, x, y, w, h);
        else ctx.drawBorder(x, y, w, h, Ui.GOLD);
    }

    static void slotHover(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, SLOT_HOT, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, 0x30FFFFFF);
    }

    static void ribbon(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) {
            int b = Math.min(RIBBON[4], w / 2);
            ctx.drawTexture(TEX, x, y, b, h, RIBBON[0], RIBBON[1], b, RIBBON[3], T, T);
            ctx.drawTexture(TEX, x + w - b, y, b, h, RIBBON[0] + RIBBON[2] - b, RIBBON[1], b, RIBBON[3], T, T);
            if (w > 2 * b) ctx.drawTexture(TEX, x + b, y, w - 2 * b, h, RIBBON[0] + b, RIBBON[1], RIBBON[2] - 2 * b, RIBBON[3], T, T);
        } else ctx.fill(x, y, x + w, y + h, 0xC0402810);
    }

    /** Дорожка прокрутки (вертикальная). */
    static void scrollTrack(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, TRACK, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.CELL);
    }

    static void scrollThumb(DrawContext ctx, int x, int y, int w, int h) {
        if (on()) nine(ctx, THUMB, x, y, w, h);
        else ctx.fill(x, y, x + w, y + h, Ui.GOLD_DARK);
    }

    /** Глиф 8x8 (константы CLOSE…PLUS) в размере size. Возвращает false, если текстур нет. */
    static boolean glyph(DrawContext ctx, int id, int x, int y, int size) {
        if (!on()) return false;
        ctx.drawTexture(TEX, x, y, size, size, id * 8, 48, 8, 8, T, T);
        return true;
    }

    private static final Kingdoms.K[] KS = Kingdoms.K.values();

    /** Королевство по цвету карты (цвета уникальны) или null. */
    static Kingdoms.K forRgb(int rgb) {
        rgb &= 0xFFFFFF;
        for (Kingdoms.K k : KS) if (k.rgb == rgb) return k;
        return null;
    }

    /** Королевство, чьё название встречается в тексте, или null. */
    static Kingdoms.K forText(String s) {
        if (s == null) return null;
        for (Kingdoms.K k : KS) if (s.contains(k.title)) return k;
        return null;
    }

    public static void emblem(DrawContext ctx, Kingdoms.K k, int x, int y, int size) {
        if (on()) {
            int i = k.ordinal();
            if (i < 12) {
                if (size <= 20) ctx.drawTexture(TEX, x, y, size, size, i * 16, 128, 16, 16, T, T);
                else ctx.drawTexture(TEX, x, y, size, size, (i % 8) * 32, 64 + (i / 8) * 32, 32, 32, T, T);
                return;
            }
        }
        ctx.fill(x, y, x + size, y + size, 0xFF000000 | k.rgb);
    }
}
