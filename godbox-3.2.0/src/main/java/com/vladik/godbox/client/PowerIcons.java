package com.vladik.godbox.client;

import com.vladik.godbox.Category;
import com.vladik.godbox.Power;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

/**
 * Собственные иконки сил и категорий (tools/icons/gen.py). Ячейка атласа = Power.ordinal() /
 * Category.ordinal(), поэтому константы ниже перезаписывает генератор; хэш порядка сверяется
 * с текущим enum, а размеры атласа — с числом ячеек. При любом расхождении иконок «нет» (false).
 */
public final class PowerIcons {
    private static final int CELL = 16;
    // Ниже — значения, выставляемые генератором (python3 tools/icons/gen.py).
    static final int COLS = 20;
    static final int POWER_COUNT = 307;
    static final int POWER_HASH = 1923478904;
    static final int CAT_COUNT = 13;
    static final int CAT_HASH = -1152260766;

    private static final Identifier POWERS = Identifier.of("godbox", "textures/gui/power_icons.png");
    private static final Identifier CATS = Identifier.of("godbox", "textures/gui/category_icons.png");

    private static boolean checked;
    private static boolean powersOk, catsOk;
    private static int powerW, powerH, catW;

    private PowerIcons() {}

    private static int[] size(Identifier id) {
        try {
            var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().getInputStream(); NativeImage img = NativeImage.read(in)) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static int hashOf(Enum<?>[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i].name());
        }
        return sb.toString().hashCode();
    }

    private static void check() {
        checked = true;
        Power[] ps = Power.values();
        Category[] cs = Category.values();
        int[] a = size(POWERS);
        int rows = (ps.length + COLS - 1) / COLS;
        powersOk = a != null && ps.length == POWER_COUNT && hashOf(ps) == POWER_HASH
                && a[0] == COLS * CELL && a[1] == rows * CELL;
        powerW = powersOk ? a[0] : 0;
        powerH = powersOk ? a[1] : 0;
        int[] b = size(CATS);
        catsOk = b != null && cs.length == CAT_COUNT && hashOf(cs) == CAT_HASH
                && b[0] == cs.length * CELL && b[1] == CELL;
        catW = catsOk ? b[0] : 0;
    }

    /** Рисует иконку силы size×size в (x, y); false — своей иконки нет, вызывающий рисует предмет. */
    public static boolean draw(DrawContext ctx, Power power, int x, int y, int size) {
        if (!checked) check();
        if (!powersOk || power == null) return false;
        int i = power.ordinal();
        ctx.drawTexture(POWERS, x, y, size, size, (i % COLS) * CELL, (i / COLS) * CELL, CELL, CELL, powerW, powerH);
        return true;
    }

    /** Иконка категории для вкладок; false — нет. */
    public static boolean drawCategory(DrawContext ctx, Category category, int x, int y, int size) {
        if (!checked) check();
        if (!catsOk || category == null) return false;
        ctx.drawTexture(CATS, x, y, size, size, category.ordinal() * CELL, 0, CELL, CELL, catW, CELL);
        return true;
    }
}
