package com.vladik.godbox;

import com.vladik.godbox.net.ChroniclePayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Летопись мира: структурированные события королевств с долгой памятью. */
public final class Chronicle {
    /** Тип события (порядок не менять: индекс хранится в сохранении и в пакетах). */
    public enum Type {
        WAR("Война", 0xE05050), DIPLOMACY("Дипломатия", 0x60C0F0), DYNASTY("Династия", 0xE0C050),
        DISASTER("Бедствие", 0xE08030), BUILD("Строительство", 0xB0A080), FAITH("Вера", 0xC080E0),
        SCIENCE("Наука и эпохи", 0x70E0A0), ECONOMY("Экономика", 0xD0D060), OTHER("Прочее", 0xA0A0A0);

        public final String label;
        public final int rgb;

        Type(String label, int rgb) {
            this.label = label;
            this.rgb = rgb;
        }

        public static Type of(int i) {
            Type[] v = values();
            return i >= 0 && i < v.length ? v[i] : OTHER;
        }
    }

    /** Запись летописи. Текст без цветовых кодов; k == null — событие мира. */
    public static final class Entry {
        public final long id;
        public final int day, year, season;
        public final Kingdoms.K k;
        public final Type type;
        public final boolean major;
        public final String text;
        public final boolean hasPos;
        public final int x, z;
        public final String dim;

        Entry(long id, int day, int year, int season, Kingdoms.K k, Type type, boolean major, String text, boolean hasPos, int x, int z, String dim) {
            this.id = id;
            this.day = day;
            this.year = year;
            this.season = season;
            this.k = k;
            this.type = type;
            this.major = major;
            this.text = text;
            this.hasPos = hasPos;
            this.x = x;
            this.z = z;
            this.dim = dim == null ? "" : dim;
        }
    }

    /** Старый вид: последние 200 строк с цветовыми кодами (Save пишет их как "L|", History берёт из них заметки). */
    static final ArrayDeque<String> LOG = new ArrayDeque<>();
    static final ArrayList<Entry> ENTRIES = new ArrayList<>();
    private static MinecraftServer server;
    /** Сколько строк записано за всё время (History берёт новые строки для кадров). */
    static long seq;
    private static long nextId = 1;
    private static boolean hasNewFormat, legacyDone;

    static final int MAX_TEXT = 300;
    private static final Map<String, Type> BY_CLASS = new HashMap<>();
    private static final Pattern LEGACY = Pattern.compile("^(?:\u00a7.)*\u0414\u0435\u043d\u044c (\\d+): (?:\u00a7.)*(.*)$", Pattern.DOTALL);

    static {
        reg(Type.WAR, "Conquest", "Siege", "SiegeEngines", "Navy", "Rebels", "Unrest");
        reg(Type.DIPLOMACY, "Diplomacy", "Treaties", "Vassals");
        reg(Type.DYNASTY, "Dynasty", "Kings", "Heroes");
        reg(Type.DISASTER, "Events", "Plague", "Tectonics", "Creatures");
        reg(Type.BUILD, "Colonies", "Highways", "Roads", "Ships", "Herds", "Repair", "Wonders", "Banners");
        reg(Type.FAITH, "Faith", "Prayers", "Magic", "Races");
        reg(Type.SCIENCE, "Tech", "Eras", "WorldAges", "Seasons", "Ages");
        reg(Type.ECONOMY, "Economy");
    }

    private Chronicle() {}

    private static void reg(Type t, String... classes) {
        for (String c : classes) BY_CLASS.put(c, t);
    }

    public static void init() {
        SaveExt.register("chron", Chronicle::save, Chronicle::load, Chronicle::reset);
    }

    public static void tick(MinecraftServer s) {
        server = s;
        if (!legacyDone) importLegacy();
    }

    static long day() {
        if (server == null || server.getOverworld() == null) return 1;
        return server.getOverworld().getTimeOfDay() / 24000L + 1;
    }

    static int max() {
        return Math.max(200, Config.i("chronicle.max", 4000));
    }

    // ---------------- запись ----------------

    /** Записать строку (можно с цветовыми кодами); королевство и тип определяются по тексту. */
    public static void log(String s) {
        if (s == null || s.isBlank()) return;
        add(detect(Formatting.strip(s)), null, null, null, s, false);
    }

    /** Событие мира или королевства с явным типом, важностью и позицией (k и pos можно null). */
    static void log(Kingdoms.K k, Type type, boolean major, BlockPos pos, String s) {
        if (s == null || s.isBlank()) return;
        add(k, type, major, pos, s, false);
    }

    /** Событие королевства: в летопись, а важное ещё и в чат. */
    static void say(MinecraftServer s, Kingdoms.K k, String msg) {
        if (s != null) server = s;
        add(k, null, null, null, msg, true);
    }

    /** То же с явным типом, важностью и позицией (pos == null — столица). */
    static void say(MinecraftServer s, Kingdoms.K k, Type type, boolean major, BlockPos pos, String msg) {
        if (s != null) server = s;
        add(k, type, major, pos, msg, true);
    }

    private static void add(Kingdoms.K k, Type forcedType, Boolean forcedMajor, BlockPos pos, String raw, boolean chat) {
        String text = Formatting.strip(raw);
        if (text == null || text.isBlank()) return;
        text = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT - 1) + "\u2026";
        long d = day();
        String line = "\u00a78\u0414\u0435\u043d\u044c " + d + ": \u00a7r" + (k != null && chat ? "\u00a7" + k.color.getCode() + k.title + "\u00a77 " + raw : raw);
        if (!LOG.isEmpty() && LOG.peekLast().equals(line)) return;
        LOG.addLast(line);
        seq++;
        while (LOG.size() > 200) LOG.removeFirst();

        Type type = forcedType;
        boolean major;
        if (forcedType == null || forcedMajor == null) {
            Cls cls = classify(caller(), text);
            if (type == null) type = cls.type;
            major = forcedMajor != null ? forcedMajor : cls.major;
        } else major = forcedMajor;

        boolean has = false;
        int x = 0, z = 0;
        String dim = "";
        if (pos != null) {
            has = true;
            x = pos.getX();
            z = pos.getZ();
        } else if (k != null && k.capital != null) {
            has = true;
            x = k.capital.getX();
            z = k.capital.getZ();
        }
        if (k != null && k.world != null) dim = k.world.getRegistryKey().getValue().toString();
        else if (has && server != null && server.getOverworld() != null) dim = server.getOverworld().getRegistryKey().getValue().toString();
        int year = 1, season = -1;
        if (Seasons.on) {
            year = Seasons.year();
            season = Seasons.index();
        } else if (server != null && server.getOverworld() != null) year = (int) (server.getOverworld().getTimeOfDay() / Life.YEAR + 1);
        append(new Entry(nextId++, (int) d, year, season, k, type, major, text, has, x, z, dim));

        if (!chat || k == null || server == null) return;
        int mode = Config.i("chronicle.chat", 1);
        if (mode <= 0 || (mode == 1 && !major)) return;
        Text t = Text.literal("[Летопись] ").formatted(Formatting.GOLD)
            .append(Text.literal(k.title).formatted(k.color))
            .append(Text.literal(" " + raw).formatted(Formatting.GRAY));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) p.sendMessage(t, false);
    }

    private static void append(Entry e) {
        ENTRIES.add(e);
        int cap = max();
        if (ENTRIES.size() <= cap + cap / 10) return;
        // сначала вытесняются самые старые незначительные записи, потом самые старые вообще
        int drop = ENTRIES.size() - cap;
        List<Entry> keep = new ArrayList<>(cap);
        for (Entry x : ENTRIES) {
            if (drop > 0 && !x.major) drop--;
            else keep.add(x);
        }
        while (drop > 0 && !keep.isEmpty()) {
            keep.remove(0);
            drop--;
        }
        ENTRIES.clear();
        ENTRIES.addAll(keep);
    }

    // ---------------- определение типа и важности ----------------

    private record Cls(Type type, boolean major) {}

    private static String caller() {
        return StackWalker.getInstance().walk(st -> st
            .map(StackWalker.StackFrame::getClassName)
            .filter(n -> !n.equals(Chronicle.class.getName()))
            .findFirst().orElse(""));
    }

    private static final String[] MAJOR = {
        "объявля", "мирный договор", "заключили союз", "пало", "захватило", "осаждает столицу", "вступает в новую эпоху",
        "наступила", "возводит чудо", "завладело чудом", "достроило замок", "основало новую религию", "гражданск",
        "восстало", "восстаёт", "империей", "вассалом", "независимость", "лишилось короля", "мятеж",
        "коронован", "династия", "унаследовал", "пришл", "вспыхнул", "эпидеми"
    };
    private static final String[] WAR = {"объявля", "захватило", "осаждает", "флот", "десант", "гражданск", "мятеж"};
    private static final String[] DIPL = {"мирный договор", "союз", "дань", "брачн", "вассал", "сюзерен"};

    private static Cls classify(String callerName, String text) {
        String c = callerName;
        int dot = c.lastIndexOf('.');
        if (dot >= 0) c = c.substring(dot + 1);
        int dollar = c.indexOf('$');
        if (dollar >= 0) c = c.substring(0, dollar);
        String low = text.toLowerCase(Locale.ROOT);
        Type t = BY_CLASS.get(c);
        if (t == null || t == Type.OTHER) t = null;
        for (String w : WAR) if (low.contains(w)) { t = Type.WAR; break; }
        if (t != Type.WAR) for (String w : DIPL) if (low.contains(w)) { t = Type.DIPLOMACY; break; }
        if (low.contains("чудо света") || low.contains("чудом света")) t = Type.BUILD;
        if (t == null) t = Type.OTHER;
        boolean major = c.equals("Dynasty") || (c.equals("Kings") && !low.contains("наследник"));
        if (!major) for (String w : MAJOR) if (low.contains(w)) { major = true; break; }
        if (c.equals("Plague") && !low.contains("пришл") && !low.contains("вспыхнул")) major = false;
        return new Cls(t, major);
    }

    /** Королевство, чьё название первым встречается в тексте (для log без королевства). */
    private static Kingdoms.K detect(String text) {
        Kingdoms.K best = null;
        int at = Integer.MAX_VALUE;
        for (Kingdoms.K k : Kingdoms.K.values()) {
            int i = text.indexOf(k.title);
            if (i >= 0 && i < at) {
                at = i;
                best = k;
            }
        }
        return best;
    }

    // ---------------- старые данные ----------------

    /** Один раз после загрузки сохранения без новой летописи: переносим строки "L|" как записи без типа. */
    private static void importLegacy() {
        legacyDone = true;
        if (hasNewFormat || !ENTRIES.isEmpty() || LOG.isEmpty()) return;
        for (String line : LOG) {
            Matcher m = LEGACY.matcher(line);
            int day = 1;
            String text = line;
            if (m.matches()) {
                try {
                    day = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                }
                text = m.group(2);
            }
            text = Formatting.strip(text);
            if (text == null || text.isBlank()) continue;
            if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT - 1) + "\u2026";
            Cls c = classify("", text);
            Kingdoms.K k = detect(text);
            ENTRIES.add(new Entry(nextId++, day, 0, -1, k, c.type, c.major, text, false, 0, 0, ""));
        }
    }

    // ---------------- сохранение: @chron|v|nextId  и  @chron|e|id|день|год|сезон|королевство|тип|важно|x|z|мир|текст ----------------

    private static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '%' -> b.append("%25");
                case '|' -> b.append("%7C");
                case '\n' -> b.append("%0A");
                case '\r' -> b.append("%0D");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    private static String unesc(String s) {
        if (s.indexOf('%') < 0) return s;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                try {
                    b.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                    i += 2;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            b.append(c);
        }
        return b.toString();
    }

    private static List<String> save() {
        List<String> o = new ArrayList<>(ENTRIES.size() + 1);
        o.add("v|" + nextId);
        for (Entry e : ENTRIES) {
            o.add("e|" + e.id + "|" + e.day + "|" + e.year + "|" + e.season + "|" + (e.k == null ? "" : e.k.id) + "|" + e.type.ordinal()
                + "|" + (e.major ? 1 : 0) + "|" + (e.hasPos ? e.x : "") + "|" + (e.hasPos ? e.z : "") + "|" + esc(e.dim) + "|" + esc(e.text));
        }
        return o;
    }

    private static void load(String[] a) {
        try {
            if (a.length >= 2 && a[0].equals("v")) {
                hasNewFormat = true;
                nextId = Math.max(nextId, Long.parseLong(a[1]));
            } else if (a.length >= 12 && a[0].equals("e")) {
                boolean has = !a[8].isEmpty() && !a[9].isEmpty();
                String text = unesc(a[11]);
                if (text.isBlank()) return;
                long id = Long.parseLong(a[1]);
                ENTRIES.add(new Entry(id, Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]), Kingdoms.byId(a[5]),
                    Type.of(Integer.parseInt(a[6])), a[7].equals("1"), text, has, has ? Integer.parseInt(a[8]) : 0, has ? Integer.parseInt(a[9]) : 0, unesc(a[10])));
                nextId = Math.max(nextId, id + 1);
                hasNewFormat = true;
            }
        } catch (RuntimeException ex) {
            GodBox.LOG.warn("GodBox: запись летописи повреждена и пропущена");
        }
    }

    private static void reset() {
        ENTRIES.clear();
        nextId = 1;
        hasNewFormat = false;
        legacyDone = false;
    }

    // ---------------- чтение ----------------

    static List<String> last(int n) {
        List<String> all = new ArrayList<>(LOG);
        return all.subList(Math.max(0, all.size() - n), all.size());
    }

    static Entry byId(long id) {
        for (int i = ENTRIES.size() - 1; i >= 0; i--) if (ENTRIES.get(i).id == id) return ENTRIES.get(i);
        return null;
    }

    static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    /** Показ летописи богу: экран у моддед-клиента, иначе последние строки в чат. */
    static void show(ServerPlayerEntity god) {
        if (ServerPlayNetworking.canSend(god, ChroniclePayloads.Open.ID)) {
            ServerPlayNetworking.send(god, new ChroniclePayloads.Open());
            return;
        }
        god.sendMessage(Text.literal("=== Летопись мира ===").formatted(Formatting.GOLD), false);
        List<String> l = last(15);
        if (l.isEmpty()) god.sendMessage(Text.literal("Пока ничего не произошло.").formatted(Formatting.GRAY), false);
        for (String s : l) god.sendMessage(Text.literal(s), false);
    }

    public static void clear() {
        LOG.clear();
        ENTRIES.clear();
        nextId = 1;
        hasNewFormat = false;
        legacyDone = false;
    }
}
