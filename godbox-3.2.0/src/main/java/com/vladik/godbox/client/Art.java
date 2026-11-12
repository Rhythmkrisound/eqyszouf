package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import net.minecraft.client.gui.DrawContext;

public final class Art {
    private Art() {}

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, Ui.BG);
    }

    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean active) {
        ctx.fill(x, y, x + w, y + h, active ? Ui.CELL_SEL : hovered ? Ui.CELL_HOVER : Ui.CELL);
    }

    public static void emblem(DrawContext ctx, Kingdoms.K k, int x, int y, int size) {
        ctx.fill(x, y, x + size, y + size, 0xFF000000 | k.rgb);
    }
}
