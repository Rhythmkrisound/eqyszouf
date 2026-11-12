package com.vladik.godbox;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Условия победы королевства: завоевание (последнее или сюзерен всех остальных), господство (доля городов,
 * удерживаемая заданное время) и чудеса света. Мир после победы продолжается; новая гонка начинается, когда победитель пал.
 */
public final class Victory {
    private static final int STEP = 200;

    private static final Set<Kingdoms.K> SEEN = EnumSet.noneOf(Kingdoms.K.class);
    private static final Set<String> CLOSE = new HashSet<>();
    private static Kingdoms.K victor;
    private static String how = "";
    private static long day;
    private static Kingdoms.K leader;
    private static int hold;
    private static int t;

    private Victory() {}

    /** Вызывается из Systems.registerDefaults (до загрузки мира). */
    static void init() {
        SaveExt.register("victory", Victory::save, Victory::load, Victory::clear);
    }

    // ---------------------------------------------------------------- расчёт

    private static int towns(Kingdoms.K k) {
        List<BlockPos> l = Conquest.TOWNS.get(k);
        return 1 + (l == null ? 0 : l.size());
    }

    private static Kingdoms.K root(Kingdoms.K k) {
        Kingdoms.K r = k;
        for (int i = 0; i < Kingdoms.K.values().length && Vassals.LORD.get(r) != null; i++) r = Vassals.LORD.get(r);
        return r;
    }

    private static List<Kingdoms.K> alive() {
        List<Kingdoms.K> l = new ArrayList<>();
        for (Kingdoms.K k : Kingdoms.K.values()) if (k.alive && k.world != null && k.capital != null) l.add(k);
        return l;
    }

    private static double share() { return Math.max(0.3, Math.min(1.0, Config.d("victory.hegemony.share", 75) / 100.0)); }

    private static int holdNeed() { return Math.max(1, Config.i("victory.hegemony.minutes", 10)) * 1200; }

    private static int wondersNeed() { return Config.i("victory.wonders", 3); }

    private static int wonders(Kingdoms.K k) {
        int n = 0;
        for (Kingdoms.K o : Wonders.OWNER.values()) if (o == k) n++;
        return n;
    }

    private static int total(List<Kingdoms.K> al) {
        int n = 0;
        for (Kingdoms.K k : al) n += towns(k);
        return n;
    }

    private static Kingdoms.K top(List<Kingdoms.K> al) {
        Kingdoms.K best = null;
        for (Kingdoms.K k : al) if (best == null || towns(k) > towns(best)) best = k;
        return best;
    }

    // ---------------------------------------------------------------- тик

    public static void tick(MinecraftServer s) {
        if (++t % STEP != 0 || !Config.b("victory.enabled", true) || s.getOverworld() == null) return;
        List<Kingdoms.K> al = alive();
        SEEN.addAll(al);
        if (victor != null) {
            if (victor.alive) return;
            Chronicle.log(victor, Chronicle.Type.OTHER, true, null, "Держава-победитель пала — гонка за господством начинается заново");
            victor = null;
            how = "";
            leader = null;
            hold = 0;
            CLOSE.clear();
            SEEN.clear();
            SEEN.addAll(al);
            return;
        }
        if (al.isEmpty()) {
            leader = null;
            hold = 0;
            return;
        }
        if (SEEN.size() >= 2) {
            for (Kingdoms.K k : al) {
                boolean all = true;
                for (Kingdoms.K o : al) if (o != k && root(o) != k) { all = false; break; }
                if (all) { win(s, k, "conquest"); return; }
            }
        }
        if (al.size() < 2) return;
        hegemony(s, al);
        if (victor != null) return;
        int need = wondersNeed();
        if (need > 0) {
            for (Kingdoms.K k : al) {
                int n = wonders(k);
                if (n >= need) { win(s, k, "wonders"); return; }
                if (need >= 2 && n == need - 1) close(s, k, "wond", "собрало " + n + " из " + need + " чудес света, нужных для победы");
            }
        }
    }

    private static void hegemony(MinecraftServer s, List<Kingdoms.K> al) {
        int tot = total(al);
        Kingdoms.K best = top(al);
        boolean thin = tot < 4 || (tot < 8 && SEEN.size() < 3);
        double sh = thin ? 0 : towns(best) / (double) tot, thr = share();
        if (sh >= thr) {
            hold = best == leader ? hold + STEP : STEP;
            leader = best;
            if (hold >= holdNeed()) { win(s, best, "hegemony"); return; }
            close(s, best, "hold", "держит " + pct(sh) + " городов — через " + Math.max(1, (holdNeed() - hold) / 1200) + " мин. наступит господство");
            return;
        }
        if (leader != null) {
            CLOSE.remove("hold:" + leader.id);
            leader = null;
            hold = 0;
        }
        for (Kingdoms.K k : al) {
            double r = thin ? 0 : towns(k) / (double) tot / thr;
            if (r >= 2.0 / 3.0) close(s, k, "heg", "захватило " + pct(towns(k) / (double) tot) + " городов — близко к господству (нужно " + pct(thr) + ")");
            else if (r < 0.5) CLOSE.remove("heg:" + k.id);
        }
    }

    private static String pct(double v) { return Math.round(v * 100) + "%"; }

    private static void close(MinecraftServer s, Kingdoms.K k, String kind, String text) {
        if (!CLOSE.add(kind + ":" + k.id)) return;
        Chronicle.say(s, k, Chronicle.Type.OTHER, true, null, text);
    }

    // ---------------------------------------------------------------- победа

    private static String howRu(String h) {
        return switch (h) {
            case "conquest" -> "завоевание мира";
            case "hegemony" -> "господство над землями";
            case "wonders" -> "расцвет культуры — чудеса света";
            default -> h;
        };
    }

    private static void win(MinecraftServer s, Kingdoms.K k, String how) {
        victor = k;
        Victory.how = how;
        day = Chronicle.day();
        leader = null;
        hold = 0;
        Chronicle.say(s, k, Chronicle.Type.OTHER, true, null, "одерживает победу: " + howRu(how) + " (день " + day + "). Мир продолжается");
        Text title = Text.literal("🏆 " + k.title).formatted(k.color, Formatting.BOLD);
        Text sub = Text.literal("Победа: " + howRu(how)).formatted(Formatting.GOLD);
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 100, 30));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(sub));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
        fireworks(k);
        Save.save(s);
    }

    private static void fireworks(Kingdoms.K k) {
        ServerWorld w = k.world;
        BlockPos c = k.capital;
        if (w == null || c == null) return;
        for (int i = 0; i < 12; i++) {
            Tasks.delay(i * 14, () -> {
                BlockPos p = c.add(w.getRandom().nextInt(25) - 12, 12 + w.getRandom().nextInt(8), w.getRandom().nextInt(25) - 12);
                if (!w.isChunkLoaded(p)) return;
                Fx.particles(w, ParticleTypes.FIREWORK, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 90, 0.3, 0.3, 0.3, 0.25);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 3f, 1f);
            });
        }
    }

    // ---------------------------------------------------------------- состояние

    /** Короткая строка для интерфейса: победитель либо лидер и прогресс. */
    public static String status() {
        if (!Config.b("victory.enabled", true)) return "Победа отключена";
        if (victor != null) return "Победа: " + victor.title + " — " + howRu(how) + ", день " + day;
        List<Kingdoms.K> al = alive();
        if (al.isEmpty()) return "Королевств нет";
        Kingdoms.K best = top(al);
        int tot = total(al);
        String s = "Лидер: " + best.title + " — " + pct(towns(best) / (double) tot) + " городов из " + pct(share());
        if (leader == best && hold > 0) s += ", удержание " + hold / 1200 + "/" + holdNeed() / 1200 + " мин";
        int need = wondersNeed();
        if (need > 0) s += "; чудес: " + wonders(best) + "/" + need;
        return s;
    }

    public static Kingdoms.K victor() { return victor; }

    private static List<String> save() {
        List<String> o = new ArrayList<>();
        if (victor != null) o.add("win|" + victor.name() + "|" + day + "|" + how);
        if (leader != null) o.add("hold|" + leader.name() + "|" + hold);
        StringBuilder b = new StringBuilder();
        for (Kingdoms.K k : SEEN) b.append(b.length() > 0 ? "," : "").append(k.name());
        if (b.length() > 0) o.add("seen|" + b);
        for (String c : CLOSE) o.add("close|" + c);
        return o;
    }

    private static void load(String[] a) {
        try {
            switch (a[0]) {
                case "win" -> { victor = Kingdoms.K.valueOf(a[1]); day = Long.parseLong(a[2]); how = a[3]; }
                case "hold" -> { leader = Kingdoms.K.valueOf(a[1]); hold = Integer.parseInt(a[2]); }
                case "seen" -> { for (String n : a[1].split(",")) SEEN.add(Kingdoms.K.valueOf(n)); }
                case "close" -> CLOSE.add(a[1]);
                default -> { }
            }
        } catch (Exception e) {
            GodBox.LOG.warn("GodBox: строка победы не разобрана: {}", String.join("|", a));
        }
    }

    public static void clear() {
        SEEN.clear();
        CLOSE.clear();
        victor = null;
        how = "";
        day = 0;
        leader = null;
        hold = 0;
        t = 0;
    }
}
