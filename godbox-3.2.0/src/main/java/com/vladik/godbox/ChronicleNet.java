package com.vladik.godbox;

import com.vladik.godbox.net.ChroniclePayloads;
import com.vladik.godbox.net.ChroniclePayloads.Dto;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** v3.2: сервер летописи — страницы, сводки, метки карты и перелёт камеры. Только для богов. */
final class ChronicleNet {
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private ChronicleNet() {}

    static void register() {
        PayloadTypeRegistry.playS2C().register(ChroniclePayloads.Open.ID, ChroniclePayloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(ChroniclePayloads.Page.ID, ChroniclePayloads.Page.CODEC);
        PayloadTypeRegistry.playS2C().register(ChroniclePayloads.Sum.ID, ChroniclePayloads.Sum.CODEC);
        PayloadTypeRegistry.playS2C().register(ChroniclePayloads.Pins.ID, ChroniclePayloads.Pins.CODEC);
        PayloadTypeRegistry.playC2S().register(ChroniclePayloads.PageReq.ID, ChroniclePayloads.PageReq.CODEC);
        PayloadTypeRegistry.playC2S().register(ChroniclePayloads.Go.ID, ChroniclePayloads.Go.CODEC);
        PayloadTypeRegistry.playC2S().register(ChroniclePayloads.SumReq.ID, ChroniclePayloads.SumReq.CODEC);
        PayloadTypeRegistry.playC2S().register(ChroniclePayloads.PinReq.ID, ChroniclePayloads.PinReq.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ChroniclePayloads.PageReq.ID, (p, ctx) -> {
            ServerPlayerEntity pl = ctx.player();
            pl.getServer().execute(() -> page(pl, p));
        });
        ServerPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Go.ID, (p, ctx) -> {
            ServerPlayerEntity pl = ctx.player();
            pl.getServer().execute(() -> go(pl, p.id()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ChroniclePayloads.SumReq.ID, (p, ctx) -> {
            ServerPlayerEntity pl = ctx.player();
            pl.getServer().execute(() -> summary(pl, p.k()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ChroniclePayloads.PinReq.ID, (p, ctx) -> {
            ServerPlayerEntity pl = ctx.player();
            pl.getServer().execute(() -> pins(pl));
        });
    }

    /** Бог и не чаще одного запроса за 4 тика. */
    private static boolean allowed(ServerPlayerEntity p) {
        if (!GodManager.isGod(p)) return false;
        int now = p.getServer().getTicks();
        Integer prev = LAST.put(p.getUuid(), now);
        if (LAST.size() > 64) LAST.keySet().removeIf(u -> p.getServer().getPlayerManager().getPlayer(u) == null);
        return prev == null || now - prev >= 4 || now < prev;
    }

    private static Dto dto(Chronicle.Entry e) {
        String t = e.text.length() > ChroniclePayloads.TEXT ? e.text.substring(0, ChroniclePayloads.TEXT) : e.text;
        return new Dto(e.id, e.day, Math.max(0, e.year), e.season, e.k == null ? "" : e.k.id, e.type.ordinal(), e.major, t, e.hasPos, e.x, e.z);
    }

    private static void page(ServerPlayerEntity p, ChroniclePayloads.PageReq r) {
        if (!allowed(p)) return;
        int kMask = r.kMask() & 0x1FFF, tMask = r.tMask() & ((1 << Chronicle.Type.values().length) - 1);
        String q = r.query() == null ? "" : Chronicle.norm(r.query().strip());
        if (q.length() > 40) q = q.substring(0, 40);
        List<Chronicle.Entry> hits = new ArrayList<>();
        for (int i = Chronicle.ENTRIES.size() - 1; i >= 0; i--) {
            Chronicle.Entry e = Chronicle.ENTRIES.get(i);
            if (r.majorOnly() && !e.major) continue;
            if (kMask != 0 && (kMask & (1 << (e.k == null ? 12 : e.k.ordinal()))) == 0) continue;
            if (tMask != 0 && (tMask & (1 << e.type.ordinal())) == 0) continue;
            if (!q.isEmpty() && !Chronicle.norm(e.text).contains(q) && (e.k == null || !Chronicle.norm(e.k.title).contains(q))) continue;
            hits.add(e);
        }
        int size = ChroniclePayloads.PAGE;
        int pages = Math.max(1, (hits.size() + size - 1) / size);
        int page = Math.max(0, Math.min(pages - 1, r.page()));
        List<Dto> out = new ArrayList<>(size);
        for (int i = page * size; i < Math.min(hits.size(), page * size + size); i++) out.add(dto(hits.get(i)));
        ServerPlayNetworking.send(p, new ChroniclePayloads.Page(page, pages, hits.size(), out));
    }

    private static void pins(ServerPlayerEntity p) {
        if (!allowed(p)) return;
        List<Dto> out = new ArrayList<>();
        for (int i = Chronicle.ENTRIES.size() - 1; i >= 0 && out.size() < ChroniclePayloads.MAX_PINS; i--) {
            Chronicle.Entry e = Chronicle.ENTRIES.get(i);
            if (e.major && e.hasPos) out.add(dto(e));
        }
        ServerPlayNetworking.send(p, new ChroniclePayloads.Pins(out));
    }

    private static void go(ServerPlayerEntity p, long id) {
        if (!allowed(p)) return;
        Chronicle.Entry e = Chronicle.byId(id);
        if (e == null || !e.hasPos) return;
        if (!e.dim.isEmpty() && !e.dim.equals(p.getServerWorld().getRegistryKey().getValue().toString())) {
            p.sendMessage(Text.literal("Это место в другом измерении").formatted(Formatting.GRAY), true);
            return;
        }
        double lim = 29_999_000;
        double x = Math.max(-lim, Math.min(lim, e.x + 0.5)), z = Math.max(-lim, Math.min(lim, e.z + 0.5));
        p.requestTeleport(x, p.getY(), z);
    }

    // ---------------- сводка по королевству ----------------

    private static String cut(String s) {
        return s.length() > ChroniclePayloads.LINE ? s.substring(0, ChroniclePayloads.LINE - 1) + "\u2026" : s;
    }

    private static String when(Chronicle.Entry e) {
        return (e.year > 0 ? e.year + " г." : "день " + e.day) + ": ";
    }

    private static void summary(ServerPlayerEntity p, String id) {
        if (!allowed(p)) return;
        Kingdoms.K k = Kingdoms.byId(id);
        List<String> o = new ArrayList<>();
        if (k != null) {
            o.add("#" + k.title);
            o.add(k.alive ? "Существует" + (k.capital != null ? ", столица " + k.capital.getX() + ", " + k.capital.getZ() : "") : "Пало или ещё не основано");
            if (k.alive) {
                o.add("Жителей: " + k.members.size());
                o.add("Король: " + Kings.kingName(k));
                o.add("Династия: " + Dynasty.describe(k));
                Civ.City c = Civ.city(k);
                if (c != null) o.add("Эпоха: " + Eras.NAMES[Math.max(0, Math.min(Eras.NAMES.length - 1, c.era))]);
                StringBuilder war = new StringBuilder(), ally = new StringBuilder();
                for (Kingdoms.K other : Kingdoms.K.values()) {
                    if (other == k || !other.alive) continue;
                    if (Diplomacy.hostile(k, other)) war.append(war.length() > 0 ? ", " : "").append(other.title);
                    else if (Diplomacy.allied(k, other)) ally.append(ally.length() > 0 ? ", " : "").append(other.title);
                }
                o.add("Война с: " + (war.length() == 0 ? "никем" : war));
                if (ally.length() > 0) o.add("Союзники: " + ally);
            }
            int founded = 0, lost = 0;
            List<String> reigns = new ArrayList<>(), wars = new ArrayList<>(), wonders = new ArrayList<>(), eras = new ArrayList<>();
            for (Chronicle.Entry e : Chronicle.ENTRIES) {
                boolean own = e.k == k, involved = own || e.text.contains(k.title);
                if (!involved) continue;
                String t = e.text.toLowerCase();
                if (own && t.contains("основало новый город")) founded++;
                if ((t.contains("захватило город королевства «" + k.title.toLowerCase()) || t.contains(("захватило " + k.title).toLowerCase())) && !own) lost++;
                if (e.type == Chronicle.Type.DYNASTY && own && (t.contains("коронован") || t.contains("унаследовал") || t.contains("пресеклась"))) reigns.add(when(e) + e.text);
                if ((e.type == Chronicle.Type.WAR || t.contains("мирный договор")) && (t.contains("объявля") || t.contains("захватило") || t.contains("мирный договор") || t.contains("пало"))) wars.add(when(e) + e.text);
                if (own && (t.contains("чудо света") || t.contains("чудом света"))) wonders.add(when(e) + e.text);
                if (own && t.contains("новую эпоху")) eras.add(when(e) + e.text);
            }
            o.add("Городов основано: " + founded + ", потеряно: " + lost);
            tail(o, "Правители и династии", reigns, 8);
            tail(o, "Войны и мир", wars, 10);
            tail(o, "Чудеса света", wonders, 5);
            tail(o, "Эпохи", eras, 4);
        }
        List<String> out = new ArrayList<>(o.size());
        for (String s : o) if (out.size() < ChroniclePayloads.MAX_LINES) out.add(cut(s));
        ServerPlayNetworking.send(p, new ChroniclePayloads.Sum(k == null ? "" : k.id, out));
    }

    private static void tail(List<String> o, String head, List<String> l, int n) {
        if (l.isEmpty()) return;
        o.add("#" + head);
        for (int i = Math.max(0, l.size() - n); i < l.size(); i++) o.add(l.get(i));
    }
}
