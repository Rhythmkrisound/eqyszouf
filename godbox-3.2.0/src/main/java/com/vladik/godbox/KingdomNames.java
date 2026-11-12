package com.vladik.godbox;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Переименование королевств: число королевств фиксировано, меняется только отображаемое название (K.title).
 * Команды: /godbox kingdom &lt;id&gt; name &lt;текст&gt; и /godbox kingdom &lt;id&gt; reset (права 2).
 * Названия клиент получает с сервера (карта, границы, история); клиентские подписи-чипы летописи берут стандартное название.
 */
public final class KingdomNames {
    public static final int MAX_LEN = 32;

    private KingdomNames() {}

    /** Вызывается из Systems.registerDefaults (до загрузки мира). */
    static void init() {
        SaveExt.register("kname", KingdomNames::save, KingdomNames::load, KingdomNames::reset);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            SuggestionProvider<ServerCommandSource> ids = (ctx, b) -> {
                for (Kingdoms.K k : Kingdoms.K.values()) b.suggest(k.id);
                return b.buildFuture();
            };
            dispatcher.register(CommandManager.literal("godbox")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.literal("kingdom")
                            .then(CommandManager.argument("id", StringArgumentType.word()).suggests(ids)
                                    .then(CommandManager.literal("name")
                                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                                    .executes(c -> name(c.getSource(), StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "text")))))
                                    .then(CommandManager.literal("reset")
                                            .executes(c -> name(c.getSource(), StringArgumentType.getString(c, "id"), null))))));
        });
    }

    /** Убирает управляющие символы, цветовые коды и разделители сохранения; режет до MAX_LEN символов. */
    public static String sanitize(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        s.codePoints().forEach(cp -> {
            if (cp == '\u00a7') return;
            if (cp == '|') b.append('/');
            else if (Character.isISOControl(cp) || Character.isWhitespace(cp)) b.append(' ');
            else b.appendCodePoint(cp);
        });
        String t = b.toString().replaceAll(" {2,}", " ").trim();
        if (t.codePointCount(0, t.length()) > MAX_LEN) t = t.substring(0, t.offsetByCodePoints(0, MAX_LEN)).trim();
        return t;
    }

    private static int name(ServerCommandSource src, String id, String text) {
        Kingdoms.K k = Kingdoms.byId(id);
        if (k == null) {
            src.sendError(Text.literal("Нет такого королевства: " + id));
            return 0;
        }
        String nt = text == null ? k.defaultTitle : sanitize(text);
        if (nt.isEmpty()) {
            src.sendError(Text.literal("Название не может быть пустым"));
            return 0;
        }
        for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != k && o.title.equalsIgnoreCase(nt)) {
                src.sendError(Text.literal("Название уже занято: " + o.id));
                return 0;
            }
        }
        String old = k.title;
        if (old.equals(nt)) {
            src.sendFeedback(() -> Text.literal("Название не изменилось: " + nt), false);
            return 1;
        }
        MinecraftServer s = src.getServer();
        apply(s, k, nt);
        Chronicle.log(k, Chronicle.Type.OTHER, true, null, "«" + old + "» отныне зовётся «" + nt + "»");
        s.getPlayerManager().broadcast(Text.literal("✎ «" + old + "» отныне зовётся «" + nt + "»").formatted(k.color), false);
        return 1;
    }

    private static void apply(MinecraftServer s, Kingdoms.K k, String title) {
        k.title = title;
        Team t = s.getScoreboard().getTeam(k.team());
        if (t != null) t.setDisplayName(Text.literal(title));
    }

    private static List<String> save() {
        List<String> out = new ArrayList<>();
        for (Kingdoms.K k : Kingdoms.K.values()) if (!k.title.equals(k.defaultTitle)) out.add(k.id + "|" + sanitize(k.title));
        return out;
    }

    private static void load(String[] a) {
        if (a.length < 2) return;
        Kingdoms.K k = Kingdoms.byId(a[0]);
        String t = sanitize(a[1]);
        if (k != null && !t.isEmpty()) k.title = t;
    }

    static void reset() {
        for (Kingdoms.K k : Kingdoms.K.values()) k.title = k.defaultTitle;
    }
}
