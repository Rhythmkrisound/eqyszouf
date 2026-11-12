package com.vladik.godbox.client;

import com.vladik.godbox.Category;
import com.vladik.godbox.Power;
import net.minecraft.client.gui.DrawContext;

public final class PowerIcons {
    private PowerIcons() {}

    /** Рисует иконку силы size×size в (x, y); false — своей иконки нет, вызывающий рисует предмет. */
    public static boolean draw(DrawContext ctx, Power power, int x, int y, int size) {
        return false;
    }

    /** Иконка категории для вкладок; false — нет. */
    public static boolean drawCategory(DrawContext ctx, Category category, int x, int y, int size) {
        return false;
    }
}
