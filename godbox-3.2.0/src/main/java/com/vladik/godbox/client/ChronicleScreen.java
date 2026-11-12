package com.vladik.godbox.client;

import com.vladik.godbox.Chronicle;
import com.vladik.godbox.Kingdoms;
import com.vladik.godbox.net.ChroniclePayloads;
import com.vladik.godbox.net.ChroniclePayloads.Dto;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** v3.2: экран летописи — фильтры, поиск, группы по годам и сезонам, сводка по королевству, перелёт к месту события. */
public class ChronicleScreen extends Screen {
    private static final String[] SEASONS = {"Весна", "Лето", "Осень", "Зима"};
    private static final Kingdoms.K[] KS = Kingdoms.K.values();
    private static final Chronicle.Type[] TYPES = Chronicle.Type.values();
    private static final int WORLD_BIT = 12;

    /** Метки для карты мира. */
    static List<Dto> pins = List.of();
    static long pinsAt;

    // фильтры сохраняются между открытиями
    private static int kMask, tMask, tab;
    private static boolean majorOnly;
    private static String query = "";

    private record Chip(int x, int y, int w, int h, int kind, int idx, String label) {}

    private record Row(Dto e, String header, List<OrderedText> lines, int y, int h) {}

    private final boolean fromGod;
    private TextFieldWidget field;
    private final List<Chip> chips = new ArrayList<>();
    private int px, py, pw, ph, listX, listY, listW, listH, tabY;

    private final List<Dto> loaded = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private int contentH, scroll, pages = 1, total, nextPage;
    private boolean loading, dragBar;
    private long sentAt, dirtyAt, sumSentAt;
    private boolean noNet;
    private List<String> sum = List.of();
    private String sumFor = "";
    private boolean sumWait;
    private List<OrderedText> sumLines = List.of();

    private ChronicleScreen(boolean fromGod) {
        super(Text.literal("Летопись"));
        this.fromGod = fromGod;
    }

    // ---------------- вход ----------------

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Open.ID, (p, ctx) -> ctx.client().execute(ChronicleScreen::open));
        ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Page.ID, (p, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().currentScreen instanceof ChronicleScreen s) s.onPage(p);
        }));
        ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Sum.ID, (p, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().currentScreen instanceof ChronicleScreen s) s.onSum(p);
        }));
        ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Pins.ID, (p, ctx) -> ctx.client().execute(() -> pins = List.copyOf(p.entries())));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            pins = List.of();
            pinsAt = 0;
        });
    }

    public static void open() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.currentScreen instanceof ChronicleScreen) return;
        boolean god = c.currentScreen instanceof GodScreen;
        if (!god && c.currentScreen != null) return;
        double mx = c.mouse.getX(), my = c.mouse.getY();
        if (c.currentScreen instanceof GodScreen gs) gs.closeSilently(); // иначе экран бога выключит режим бога
        c.setScreen(new ChronicleScreen(god));
        InputUtil.setCursorParameters(c.getWindow().getHandle(), InputUtil.GLFW_CURSOR_NORMAL, mx, my);
    }

    @Override
    public void close() {
        if (client == null) return;
        client.setScreen(fromGod && client.player != null ? new GodScreen() : null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------------- раскладка ----------------

    @Override
    protected void init() {
        pw = Math.min(width - 16, 700);
        px = (width - pw) / 2;
        py = 8;
        ph = height - 16;
        field = new TextFieldWidget(textRenderer, px + 10, py + 22, Math.max(60, Math.min(160, pw / 3)), 14, Text.literal("Поиск"));
        field.setMaxLength(40);
        field.setPlaceholder(Text.literal("Поиск…").formatted(Formatting.DARK_GRAY));
        field.setText(query);
        field.setChangedListener(s -> {
            query = s;
            dirtyAt = System.currentTimeMillis();
        });
        addSelectableChild(field);

        chips.clear();
        int right = px + pw - 10;
        int tw = Math.min(70, (pw - 20) / 2 - 80);
        tabY = py + 5;
        if (tw > 30) {
            chips.add(new Chip(right - tw * 2 - 2, tabY, tw, 14, 4, 0, "Записи"));
            chips.add(new Chip(right - tw, tabY, tw, 14, 5, 0, "Сводка"));
        }
        int fx = field.getX() + field.getWidth() + 6;
        String maj = "★ Важные";
        int mw = textRenderer.getWidth(maj) + 12;
        chips.add(new Chip(fx, py + 21, mw, 16, 2, 0, maj));
        int x = px + 10, y = py + 41, row = 18;
        for (int i = 0; i <= WORLD_BIT; i++) {
            if (x + 16 > right) { x = px + 10; y += row; }
            chips.add(new Chip(x, y, 16, 16, 0, i, i == WORLD_BIT ? "События мира" : KS[i].title));
            x += 18;
        }
        x += 4;
        for (int i = 0; i < TYPES.length; i++) {
            int w = textRenderer.getWidth(TYPES[i].label) + 10;
            if (x + w > right) { x = px + 10; y += row; }
            chips.add(new Chip(x, y, w, 16, 1, i, TYPES[i].label));
            x += w + 2;
        }
        listX = px + 8;
        listY = y + row + 2;
        listW = pw - 16 - 8;
        listH = Math.max(20, py + ph - 8 - listY);
        if (loaded.isEmpty() && !loading) refresh();
        else rebuild();
    }

    // ---------------- запросы ----------------

    private void refresh() {
        nextPage = 0;
        scroll = 0;
        if (tab == 1) {
            askSum();
            return;
        }
        loading = true;
        sendPage(0);
    }

    private void sendPage(int page) {
        sentAt = System.currentTimeMillis();
        if (ClientPlayNetworking.canSend(ChroniclePayloads.PageReq.ID))
            ClientPlayNetworking.send(new ChroniclePayloads.PageReq(page, kMask, tMask, majorOnly, query));
        else {
            loading = false;
            noNet = true;
        }
    }

    private int sumKingdom() {
        for (int i = 0; i < KS.length; i++) if ((kMask & (1 << i)) != 0) return i;
        return -1;
    }

    private void askSum() {
        int i = sumKingdom();
        sumFor = i < 0 ? "" : KS[i].id;
        sum = List.of();
        sumLines = List.of();
        sumWait = i >= 0;
        scroll = 0;
        if (i >= 0) sendSum();
    }

    private void sendSum() {
        sumSentAt = System.currentTimeMillis();
        if (ClientPlayNetworking.canSend(ChroniclePayloads.SumReq.ID)) ClientPlayNetworking.send(new ChroniclePayloads.SumReq(sumFor));
        else {
            sumWait = false;
            noNet = true;
        }
    }

    private void onPage(ChroniclePayloads.Page p) {
        if (p.page() == 0) {
            loaded.clear();
        } else if (p.page() != nextPage) {
            return;
        }
        loaded.addAll(p.entries());
        pages = p.pages();
        total = p.total();
        nextPage = p.page() + 1;
        loading = false;
        rebuild();
    }

    private void onSum(ChroniclePayloads.Sum p) {
        if (!p.k().equals(sumFor)) return;
        sum = p.lines();
        sumWait = false;
        rebuild();
    }

    // ---------------- строки списка ----------------

    private void rebuild() {
        int textW = listW - 8 - 18 - 34 - 6;
        List<Row> out = new ArrayList<>();
        int y = 0;
        int lastYear = Integer.MIN_VALUE, lastSeason = -2;
        for (Dto e : loaded) {
            if (e.year() != lastYear || e.season() != lastSeason) {
                lastYear = e.year();
                lastSeason = e.season();
                String h = e.year() <= 0 ? "Старые записи" : "Год " + e.year() + (e.season() >= 0 && e.season() < 4 ? " · " + SEASONS[e.season()] : "");
                out.add(new Row(null, h, List.of(), y, 15));
                y += 15;
            }
            List<OrderedText> ls = new ArrayList<>(textRenderer.wrapLines(Text.literal(e.text()), Math.max(40, textW)));
            if (ls.size() > 4) ls = new ArrayList<>(ls.subList(0, 4));
            int h = Math.max(16, ls.size() * 10 + 6);
            out.add(new Row(e, null, ls, y, h));
            y += h;
        }
        rows = out;
        int sumH = 0;
        if (!sum.isEmpty()) {
            List<OrderedText> sl = new ArrayList<>();
            for (String s : sum) {
                if (s.startsWith("#")) sl.add(null);
                sl.addAll(textRenderer.wrapLines(Text.literal(s.startsWith("#") ? s.substring(1) : s), Math.max(40, listW - 50)));
            }
            sumLines = sl;
            sumH = sl.size() * 11 + 8;
        }
        contentH = tab == 1 ? sumH : y;
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, contentH - listH));
    }

    // ---------------- отрисовка ----------------

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x90000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        long now = System.currentTimeMillis();
        if (dirtyAt > 0 && now - dirtyAt > 350) {
            dirtyAt = 0;
            if (tab == 0) refresh();
        }
        if (tab == 0 && loading && now - sentAt > 1500) sendPage(nextPage);
        if (tab == 1 && sumWait && now - sumSentAt > 1500) sendSum();
        if (tab == 0 && !noNet && !loading && nextPage < pages && contentH - scroll - listH < 120) {
            loading = true;
            sendPage(nextPage);
        }
        Art.panel(ctx, px, py, pw, ph);
        ctx.drawTextWithShadow(textRenderer, "Летопись мира", px + 10, py + 8, Ui.GOLD);
        if (tab == 0 && total > 0 && pw > 300) ctx.drawTextWithShadow(textRenderer, total + " зап.", px + 10 + textRenderer.getWidth("Летопись мира") + 8, py + 8, Ui.DIM);
        field.render(ctx, mx, my, delta);

        List<Text> tip = null;
        for (Chip c : chips) {
            boolean hov = Ui.in(mx, my, c.x, c.y, c.w, c.h);
            boolean act = switch (c.kind) {
                case 0 -> (kMask & (1 << c.idx)) != 0;
                case 1 -> (tMask & (1 << c.idx)) != 0;
                case 2 -> majorOnly;
                case 4 -> tab == 0;
                default -> tab == 1;
            };
            Art.button(ctx, c.x, c.y, c.w, c.h, hov, act);
            if (c.kind == 0) {
                if (c.idx == WORLD_BIT) ctx.drawCenteredTextWithShadow(textRenderer, "М", c.x + c.w / 2, c.y + 4, act ? Ui.GOLD_LIGHT : Ui.TEXT);
                else Art.emblem(ctx, KS[c.idx], c.x + 2, c.y + 2, 12);
                if (hov) tip = List.of(Text.literal(c.label));
            } else {
                int col = c.kind == 1 ? 0xFF000000 | TYPES[c.idx].rgb : act ? Ui.GOLD_LIGHT : Ui.TEXT;
                ctx.drawCenteredTextWithShadow(textRenderer, c.label, c.x + c.w / 2, c.y + (c.h - 8) / 2, act && c.kind == 1 ? Ui.WHITE : col);
            }
        }

        ctx.enableScissor(listX, listY, listX + listW + 8, listY + listH);
        Dto hover = null;
        if (tab == 0) hover = renderEntries(ctx, mx, my);
        else renderSummary(ctx);
        ctx.disableScissor();
        renderBar(ctx);

        if (hover != null) {
            List<Text> t = new ArrayList<>();
            t.add(Text.literal((hover.year() > 0 ? "Год " + hover.year() + ", " : "") + "день " + hover.day()).formatted(Formatting.GOLD));
            t.add(Text.literal(TYPES[MathHelper.clamp(hover.type(), 0, TYPES.length - 1)].label + (hover.major() ? " · важное" : "")).formatted(Formatting.GRAY));
            t.add(Text.literal(hover.hasPos() ? "Клик: перелететь (" + hover.x() + ", " + hover.z() + ")" : "Место не указано").formatted(Formatting.DARK_GRAY));
            ctx.drawTooltip(textRenderer, t, mx, my);
        } else if (tip != null) {
            ctx.drawTooltip(textRenderer, tip, mx, my);
        }
    }

    private Dto renderEntries(DrawContext ctx, int mx, int my) {
        Dto hover = null;
        if (rows.isEmpty()) {
            String m = noNet ? "Сервер не поддерживает летопись" : loading ? "Загрузка…" : (kMask | tMask) != 0 || majorOnly || !query.isEmpty() ? "Ничего не найдено" : "Летопись пока пуста";
            ctx.drawCenteredTextWithShadow(textRenderer, m, listX + listW / 2, listY + Math.min(40, listH / 2), Ui.DIM);
            return null;
        }
        for (Row r : rows) {
            int y = listY + r.y - scroll;
            if (y + r.h < listY || y > listY + listH) continue;
            if (r.e == null) {
                ctx.fill(listX, y + 2, listX + listW, y + 13, 0x60B8860B);
                ctx.drawTextWithShadow(textRenderer, Ui.ellipsis(textRenderer, r.header, listW - 8), listX + 5, y + 3, Ui.GOLD_LIGHT);
                continue;
            }
            Dto e = r.e;
            Kingdoms.K k = Kingdoms.byId(e.k());
            int col = k != null ? 0xFF000000 | k.rgb : 0xFF808090;
            boolean hov = my >= listY && my < listY + listH && Ui.in(mx, my, listX, y, listW, r.h);
            ctx.fill(listX, y, listX + listW, y + r.h - 1, hov && e.hasPos() ? 0x50FFFFFF : 0x28000000);
            ctx.fill(listX, y, listX + 2, y + r.h - 1, col);
            if (k != null) Art.emblem(ctx, k, listX + 5, y + 2, 12);
            int tx = listX + 5 + 14 + 2;
            ctx.drawTextWithShadow(textRenderer, "д." + e.day(), tx, y + 3, Ui.DIM);
            int textX = tx + 34;
            int tc = e.major() ? 0xFFFFFFFF : 0xFFA8A8B8;
            for (int i = 0; i < r.lines.size(); i++) {
                ctx.drawTextWithShadow(textRenderer, r.lines.get(i), textX + (i == 0 && e.major() ? 9 : 0), y + 3 + i * 10, tc);
            }
            if (e.major()) ctx.drawTextWithShadow(textRenderer, "★", textX, y + 3, Ui.GOLD);
            int tcol = 0xFF000000 | TYPES[MathHelper.clamp(e.type(), 0, TYPES.length - 1)].rgb;
            ctx.fill(listX + listW - 4, y + 2, listX + listW - 1, y + r.h - 3, tcol);
            if (hov) hover = e;
        }
        return hover;
    }

    private void renderSummary(DrawContext ctx) {
        int i = sumKingdom();
        if (i < 0) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Выберите королевство среди значков выше", listX + listW / 2, listY + Math.min(40, listH / 2), Ui.DIM);
            return;
        }
        if (sumWait || sumLines.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, noNet ? "Сервер не поддерживает летопись" : "Загрузка…", listX + listW / 2, listY + Math.min(40, listH / 2), Ui.DIM);
            return;
        }
        Art.emblem(ctx, KS[i], listX + listW - 38, listY + 4 - scroll, 32);
        int y = listY + 4 - scroll;
        boolean head = false;
        for (OrderedText t : sumLines) {
            if (t == null) {
                head = true;
                y += 3;
                continue;
            }
            if (y + 10 >= listY && y <= listY + listH) {
                if (head) ctx.fill(listX, y - 1, listX + listW - 40, y + 10, 0x60B8860B);
                ctx.drawTextWithShadow(textRenderer, t, listX + 5, y, head ? Ui.GOLD_LIGHT : Ui.TEXT);
            }
            head = false;
            y += 11;
        }
    }

    private int[] bar() {
        int max = Math.max(0, contentH - listH);
        if (max == 0) return null;
        int th = Math.max(14, listH * listH / Math.max(listH, contentH));
        int ty = listY + (int) ((long) (listH - th) * scroll / max);
        return new int[]{listX + listW + 2, ty, 6, th};
    }

    private void renderBar(DrawContext ctx) {
        int[] b = bar();
        if (b == null) return;
        Art.scrollTrack(ctx, listX + listW + 2, listY, 6, listH);
        Art.scrollThumb(ctx, b[0], b[1], b[2], b[3]);
    }

    // ---------------- ввод ----------------

    private void setScrollFromBar(double my) {
        int[] b = bar();
        int max = Math.max(0, contentH - listH);
        if (b == null) return;
        double k = (my - listY - b[3] / 2.0) / Math.max(1, listH - b[3]);
        scroll = (int) MathHelper.clamp(k * max, 0, max);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int[] b = bar();
            if (b != null && Ui.in(mx, my, listX + listW, listY, 10, listH)) {
                dragBar = true;
                setScrollFromBar(my);
                return true;
            }
            for (Chip c : chips) {
                if (!Ui.in(mx, my, c.x, c.y, c.w, c.h)) continue;
                Ui.click();
                chip(c);
                field.setFocused(false);
                setFocused(null);
                return true;
            }
            if (tab == 0 && Ui.in(mx, my, listX, listY, listW, listH)) {
                for (Row r : rows) {
                    int y = listY + r.y - scroll;
                    if (r.e != null && my >= y && my < y + r.h) {
                        field.setFocused(false);
                        setFocused(null);
                        if (r.e.hasPos() && ClientPlayNetworking.canSend(ChroniclePayloads.Go.ID)) {
                            ClientPlayNetworking.send(new ChroniclePayloads.Go(r.e.id()));
                            Ui.click();
                            close();
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void chip(Chip c) {
        switch (c.kind) {
            case 0 -> {
                kMask = tab == 1 ? (kMask == (1 << c.idx) ? 0 : 1 << c.idx) : kMask ^ (1 << c.idx);
                refresh();
            }
            case 1 -> {
                tMask ^= 1 << c.idx;
                refresh();
            }
            case 2 -> {
                majorOnly = !majorOnly;
                refresh();
            }
            case 4 -> {
                if (tab != 0) {
                    tab = 0;
                    refresh();
                }
            }
            default -> {
                if (tab != 1) {
                    tab = 1;
                    int first = sumKingdom();
                    kMask = first < 0 ? 0 : 1 << first;
                    refresh();
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragBar) {
            setScrollFromBar(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragBar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = MathHelper.clamp(scroll - (int) (vertical * 24), 0, Math.max(0, contentH - listH));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (!field.isFocused()) {
            if (GodKeys.is(GodKeys.CHRONICLE, key, scan)) {
                close();
                return true;
            }
            int step = key == GLFW.GLFW_KEY_PAGE_DOWN ? listH - 20 : key == GLFW.GLFW_KEY_PAGE_UP ? -(listH - 20) : key == GLFW.GLFW_KEY_DOWN ? 20 : key == GLFW.GLFW_KEY_UP ? -20 : 0;
            if (step != 0) {
                scroll = MathHelper.clamp(scroll + step, 0, Math.max(0, contentH - listH));
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }
}
