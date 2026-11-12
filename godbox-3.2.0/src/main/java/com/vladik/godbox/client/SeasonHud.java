package com.vladik.godbox.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** v3.2: компактный индикатор сезона под панелью эпохи: значок, название, год, полоска до смены. */
public final class SeasonHud {
    private static final Identifier ICONS = Identifier.of("godbox", "textures/gui/seasons.png");
    private static final String[] NAMES = {"Весна", "Лето", "Осень", "Зима"};
    private static final int[] RGB = {0x7ED957, 0xFFD54A, 0xE98A2A, 0x9AD0FF};
    static final int H = 16;

    private SeasonHud() {}

    /** Сколько места снизу от панелей занимает индикатор (для смещения подсказок). */
    public static int h() { return SeasonClient.on && SeasonClient.updated != 0 ? H + 2 : 0; }

    public static void render(DrawContext ctx, TextRenderer tr, int width, int mx, int my) {
        if (h() == 0) return;
        int s = SeasonClient.season;
        String label = NAMES[s] + " · год " + SeasonClient.year;
        int w = Math.min(width - 24, 14 + 6 + tr.getWidth(label) + 8 + 46 + 8);
        int x = width / 2 - w / 2, y = WorldPanels.bottom() + 2;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + H;
        ctx.fill(x - 1, y - 1, x + w + 1, y + H + 1, 0xB0101018);
        if (hov) ctx.fill(x, y, x + w, y + H, 0x30FFFFFF);
        ctx.drawTexture(ICONS, x + 3, y + 1, 14, 14, s * 16, 0, 16, 16, 64, 16);
        int col = 0xFF000000 | RGB[s];
        ctx.drawTextWithShadow(tr, label, x + 21, y + 4, col);
        int bx = x + w - 54, bw = 46;
        ctx.fill(bx, y + 6, bx + bw, y + 10, 0xFF202020);
        ctx.fill(bx, y + 6, bx + (int) (bw * SeasonClient.progress), y + 10, col);
        if (hov) {
            int left = (int) ((1f - SeasonClient.progress) * SeasonClient.length / 20f);
            List<Text> tip = new ArrayList<>();
            tip.add(Text.literal(NAMES[s] + ", год " + SeasonClient.year).formatted(Formatting.GOLD));
            tip.add(Text.literal("До смены сезона: " + left / 60 + " мин " + left % 60 + " с").formatted(Formatting.GRAY));
            if (!SeasonClient.effects.isEmpty()) tip.add(Text.literal(SeasonClient.effects).formatted(Formatting.YELLOW));
            ctx.drawTooltip(tr, tip, mx, my + 16);
        }
    }
}
