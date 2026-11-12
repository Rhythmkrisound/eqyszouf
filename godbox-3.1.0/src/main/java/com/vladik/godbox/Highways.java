package com.vladik.godbox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Великие тракты: широкие мощёные дороги между дальними поселениями, союзниками и сюзеренами.
 * Бордюры, насыпи и выемки, мосты на опорах с перилами, фонари, аллеи деревьев,
 * верстовые столбы со знамёнами и постоялые дворы. Облик зависит от эпохи королевства.
 * Тот же движок (узкий режим) прокладывает и обычные дороги Roads. Готовая трасса хранится целиком
 * (путь, профиль высот, якоря фонарей) и перекладывается в стиле новой эпохи целиком.
 */
public final class Highways {
    public static boolean on = true;
    static final Set<String> DONE = new HashSet<>();
    /** Все трассы: строящиеся и готовые (готовые нужны для смены облика и добивки пропусков). */
    static final List<Way> WAYS = new ArrayList<>();
    static final Map<Kingdoms.K, Integer> BUILT = new EnumMap<>(Kingdoms.K.class);
    static final String[] SIDE = {"Восточный", "Юго-восточный", "Южный", "Юго-западный", "Западный", "Северо-западный", "Северный", "Северо-восточный"};
    static final String[] NOUN = {"тракт", "большак", "королевский путь", "торговый путь", "шлях", "старый путь"};
    private static final int NONE = RoadNet.NONE;
    private static final int LOOK = 20, BATCH = 6;
    private static int t;
    private static boolean registered;

    private Highways() {}

    /** Регистрирует сохранение трасс; вызывать из GodBox.onInitialize до загрузки мира. */
    public static void init() {
        if (registered) return;
        registered = true;
        SaveExt.register("roadnet", Highways::saveLines, Highways::loadLine, WAYS::clear);
    }

    static final class Way {
        final Kingdoms.K k;
        ServerWorld w;
        final String key, name;
        final boolean narrow;
        final int n;
        final int[] xs, zs, hy, deck;
        final boolean[] wt;
        /** Эпоха, в которой точка уложена, плюс 1 (0 — ещё не уложена). */
        final byte[] pe;
        final double[] tx, tz;
        final Map<Integer, Integer> lampY = new HashMap<>(), mileY = new HashMap<>();
        int i, era;
        boolean inn, done, fin;
        Place last;
        Set<Long> corr;

        Way(Kingdoms.K k, ServerWorld w, String key, String name, boolean narrow, List<int[]> pts, int era) {
            this.k = k;
            this.w = w;
            this.key = key;
            this.name = name;
            this.narrow = narrow;
            this.era = era;
            n = pts.size();
            xs = new int[n];
            zs = new int[n];
            hy = new int[n];
            deck = new int[n];
            wt = new boolean[n];
            pe = new byte[n];
            tx = new double[n];
            tz = new double[n];
            for (int j = 0; j < n; j++) {
                xs[j] = pts.get(j)[0];
                zs[j] = pts.get(j)[1];
                hy[j] = NONE;
                deck[j] = NONE;
            }
            for (int j = 0; j < n; j++) {
                int a = Math.max(0, j - 3), b = Math.min(n - 1, j + 3);
                double dx = xs[b] - xs[a], dz = zs[b] - zs[a], l = Math.sqrt(dx * dx + dz * dz);
                if (l < 1e-6) { dx = 1; dz = 0; l = 1; }
                tx[j] = dx / l;
                tz[j] = dz / l;
            }
        }

        double rad() { return narrow ? 2.0 : 2.7; }

        /** Граница проезжей части: у узкой дороги кромка дальше неё — только перила моста. */
        double lane() { return narrow ? 1.5 : 1.7; }
    }

    /** Ставит блоки порциями, затем соединяет заборы, стены и стёкла. */
    static final class Place implements Tasks.Task {
        final ServerWorld w;
        final List<BlockPos> ps = new ArrayList<>();
        final List<BlockState> ss = new ArrayList<>();
        int i, fix;

        Place(ServerWorld w) { this.w = w; }

        void put(BlockPos p, BlockState s) { ps.add(p); ss.add(s); }

        void put(BlockPos p, Block b) { put(p, b.getDefaultState()); }

        boolean finished() { return i >= ps.size() && fix >= ps.size(); }

        @Override
        public boolean tick() {
            try {
                return step();
            } catch (Exception e) {
                GodBox.LOG.error("GodBox: сбой укладки дороги", e);
                i = fix = ps.size();
                return true;
            }
        }

        private boolean step() {
            for (int n = 0; n < 40 && i < ps.size(); n++, i++) {
                BlockPos p = ps.get(i);
                if (!w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) continue;
                if (w.getBlockState(p).hasBlockEntity() && !ss.get(i).hasBlockEntity()) continue;
                w.setBlockState(p, ss.get(i), Block.NOTIFY_ALL);
            }
            if (i < ps.size()) return false;
            for (int n = 0; n < 120 && fix < ps.size(); n++, fix++) {
                BlockPos p = ps.get(fix);
                if (!w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) continue;
                BlockState s = w.getBlockState(p);
                Block b = s.getBlock();
                if (b instanceof WallBlock || b instanceof FenceBlock || b instanceof PaneBlock)
                    w.setBlockState(p, Block.postProcessState(s, w, p), Block.NOTIFY_LISTENERS);
            }
            return fix >= ps.size();
        }
    }

    // ---------------- материалы по эпохам ----------------
    static BlockState road(int era, boolean edge, int i, Random r) {
        Block b;
        if (era <= 1) b = edge ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH;
        else if (era <= 3) b = edge ? Blocks.COBBLESTONE : (r.nextInt(4) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
        else if (era <= 5) b = edge ? Blocks.STONE_BRICKS : (r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
        else if (era <= 7) b = edge ? Blocks.POLISHED_ANDESITE : (r.nextInt(6) == 0 ? (r.nextBoolean() ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS) : Blocks.STONE_BRICKS);
        else if (era == 8) b = edge ? Blocks.STONE_BRICKS : (i % 6 == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.SMOOTH_STONE);
        else if (era == 9) b = edge ? Blocks.POLISHED_DEEPSLATE : (i % 9 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.DEEPSLATE_TILES);
        else b = edge ? Blocks.POLISHED_BLACKSTONE_BRICKS : (i % 8 == 0 ? Blocks.WAXED_CUT_COPPER : Blocks.BRICKS);
        return b.getDefaultState();
    }

    static BlockState fill(int era) {
        if (era < 4) return Blocks.DIRT.getDefaultState();
        if (era < 9) return Blocks.STONE_BRICKS.getDefaultState();
        if (era == 9) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
        return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    }

    static BlockState bridge(int era, boolean edge) {
        if (era < 4) return (edge ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS).getDefaultState();
        if (era < 8) return (edge ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState();
        if (era == 8) return (edge ? Blocks.QUARTZ_BRICKS : Blocks.SMOOTH_STONE).getDefaultState();
        if (era == 9) return (edge ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).getDefaultState();
        return (edge ? Blocks.WAXED_CUT_COPPER : Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState();
    }

    static BlockState rail(int era) {
        if (era < 4) return Blocks.SPRUCE_FENCE.getDefaultState();
        if (era < 8) return Blocks.STONE_BRICK_WALL.getDefaultState();
        if (era == 8) return Blocks.STONE_BRICK_WALL.getDefaultState();
        if (era == 9) return Blocks.DEEPSLATE_BRICK_WALL.getDefaultState();
        return Blocks.IRON_BARS.getDefaultState();
    }

    static BlockState pillar(int era) {
        if (era < 4) return Blocks.SPRUCE_LOG.getDefaultState();
        if (era < 9) return Blocks.STONE_BRICKS.getDefaultState();
        if (era == 9) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
        return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    }

    static Block banner(Kingdoms.K k) {
        return k.banner();
    }

    /** Для ускорения жителей на дороге. */
    static boolean paved(BlockState s) {
        return s.isOf(Blocks.POLISHED_ANDESITE) || s.isOf(Blocks.SMOOTH_STONE) || s.isOf(Blocks.CHISELED_STONE_BRICKS) || s.isOf(Blocks.DEEPSLATE_TILES)
                || s.isOf(Blocks.POLISHED_DEEPSLATE) || s.isOf(Blocks.AMETHYST_BLOCK) || s.isOf(Blocks.BRICKS) || s.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || s.isOf(Blocks.SPRUCE_PLANKS) || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.WAXED_CUT_COPPER) || s.isOf(Blocks.STRIPPED_SPRUCE_LOG);
    }


    // ---------------- трасса ----------------
    static String name(BlockPos a, BlockPos b) {
        double ang = Math.atan2(b.getZ() - a.getZ(), b.getX() - a.getX());
        int sec = Math.floorMod((int) Math.round(ang / (Math.PI / 4)), 8);
        int h = Math.floorMod(a.getX() * 7 + b.getZ() * 13 + b.getX(), NOUN.length);
        return SIDE[sec] + " " + NOUN[h] + " к «" + Colonies.name(b) + "»";
    }

    /** Рельеф для обхода: здания, лава и грядки закрыты или дороги, вода и чужие постройки — с доплатой. */
    static RoadNet.Terrain terrain(ServerWorld w, BlockPos a, BlockPos b, List<Civ.Building> bs, int margin) {
        return new RoadNet.Terrain() {
            @Override
            public int height(int x, int z) {
                if (!w.isChunkLoaded(x >> 4, z >> 4)) return NONE;
                int g = TownBuilder.ground(w, x, z);
                return w.getBlockState(new BlockPos(x, g - 1, z)).getFluidState().isEmpty() ? g - 1 : g;
            }

            @Override
            public double pen(int x, int z) {
                if (!w.isChunkLoaded(x >> 4, z >> 4)) return 0.2;
                if (Math.max(Math.abs(x - a.getX()), Math.abs(z - a.getZ())) < 14 || Math.max(Math.abs(x - b.getX()), Math.abs(z - b.getZ())) < 14) return 0;
                for (Civ.Building q : bs) if (x >= q.x0 - margin && x <= q.x1 + margin && z >= q.z0 - margin && z <= q.z1 + margin) return -1;
                int g = TownBuilder.ground(w, x, z);
                BlockState s = w.getBlockState(new BlockPos(x, g - 1, z));
                if (s.isOf(Blocks.LAVA)) return -1;
                if (!s.getFluidState().isEmpty()) return 2.0;
                return Roads.terrain(s) || Roads.fam(s) ? 0 : 3.0;
            }
        };
    }

    static Way plan(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b, String key, boolean narrow) {
        double dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < (narrow ? 20 : 30) || len > 2500) return null;
        List<int[]> wp = RoadNet.route(terrain(w, a, b, allBuildings(), narrow ? 3 : 4), a.getX(), a.getZ(), b.getX(), b.getZ(), 3000);
        if (wp == null) wp = List.of(new int[]{a.getX(), a.getZ()}, new int[]{b.getX(), b.getZ()});
        List<int[]> pts = RoadNet.smooth(wp);
        if (pts.size() < 10) return null;
        return new Way(k, w, key, name(a, b), narrow, pts, Civ.city(k).era);
    }

    /** Высота верха земли в точке трассы (над водой — уровень настила); NONE, если чанк не загружен. */
    static int raw(Way wy, int j) {
        if (wy.hy[j] != NONE) return wy.hy[j];
        int x = wy.xs[j], z = wy.zs[j];
        if (wy.w == null || !wy.w.isChunkLoaded(x >> 4, z >> 4)) return NONE;
        int g = TownBuilder.ground(wy.w, x, z);
        boolean fl = !wy.w.getBlockState(new BlockPos(x, g - 1, z)).getFluidState().isEmpty();
        wy.wt[j] = fl;
        wy.hy[j] = fl ? g : g - 1;
        return wy.hy[j];
    }

    /** Считает профиль высот для ещё не рассчитанных точек [from,to) с запасом вперёд. */
    static void ensureDeck(Way wy, int from, int to) {
        to = Math.min(wy.n, to);
        for (int j = Math.max(0, from - LOOK); j < Math.min(wy.n, to + LOOK); j++) raw(wy, j);
        for (int j = from; j < to; ) {
            if (wy.deck[j] != NONE || wy.hy[j] == NONE) { j++; continue; }
            int e = j;
            while (e < to && wy.deck[e] == NONE && wy.hy[e] != NONE) e++;
            RoadNet.solveDeck(wy.hy, wy.wt, wy.deck, j, e, LOOK);
            j = e;
        }
    }

    static void clearUp(ServerWorld w, Place pl, BlockPos p) {
        BlockState u = w.getBlockState(p);
        if (u.isAir() || !u.getFluidState().isEmpty()) return;
        if (u.isReplaceable() || u.isIn(BlockTags.LEAVES) || u.isIn(BlockTags.LOGS) || u.isOf(Blocks.SNOW) || u.isIn(BlockTags.FLOWERS))
            pl.put(p, Blocks.AIR);
    }

    static List<Civ.Building> allBuildings() {
        List<Civ.Building> bs = new ArrayList<>();
        for (Kingdoms.K o : Kingdoms.K.values()) if (o.alive) bs.addAll(Civ.city(o).buildings);
        return bs;
    }

    /** Клетки в полосе трассы: декор не должен попадать на полотно, в том числе на ещё не уложенное. */
    static Set<Long> corridor(Way wy) {
        if (wy.corr != null) return wy.corr;
        Set<Long> c = new HashSet<>();
        int rc = (int) Math.ceil(wy.rad()) + 1;
        for (int j = 0; j < wy.n; j++)
            for (int dx = -rc; dx <= rc; dx++) for (int dz = -rc; dz <= rc; dz++) c.add(BlockPos.asLong(wy.xs[j] + dx, 0, wy.zs[j] + dz));
        return wy.corr = c;
    }

    static boolean free(Way wy, List<Civ.Building> bs, int x, int z) {
        return wy.w.isChunkLoaded(x >> 4, z >> 4) && !Roads.inBuilding(bs, x, z) && !corridor(wy).contains(BlockPos.asLong(x, 0, z))
                && TownBuilder.dry(wy.w, x, z);
    }

    /** Опора вниз до твёрдого грунта (через воду, воздух и заменяемые блоки). */
    static void pillarDown(ServerWorld w, Place pl, int era, int x, int y, int z) {
        for (int yy = y, c = 0; c < 48 && yy > w.getBottomY(); yy--, c++) {
            BlockPos p = new BlockPos(x, yy, z);
            BlockState st = w.getBlockState(p);
            if (st.getFluidState().isEmpty() && !st.isAir() && !st.isReplaceable()) break;
            pl.put(p, pillar(era));
        }
    }

    static boolean pillarBlock(BlockState s) {
        return s.isOf(Blocks.SPRUCE_LOG) || s.isOf(Blocks.STONE_BRICKS) || s.isOf(Blocks.DEEPSLATE_BRICKS) || s.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    static boolean railBlock(BlockState s) {
        return s.isAir() || s.getBlock() instanceof FenceBlock || s.getBlock() instanceof WallBlock || s.isOf(Blocks.IRON_BARS);
    }

    /**
     * Укладывает одну точку трассы: диск клеток вокруг неё (без дыр на диагоналях, постоянная ширина).
     * Клетку красит ближайшая точка. true — все клетки обработаны (иначе точку повторим, когда загрузятся чанки).
     */
    static boolean point(Way wy, int i, int era, Place pl, List<Civ.Building> bs) {
        ServerWorld w = wy.w;
        int x = wy.xs[i], z = wy.zs[i], skip = wy.narrow ? 8 : 12;
        byte target = (byte) (era + 1);
        if (Math.max(Math.abs(x - wy.xs[0]), Math.abs(z - wy.zs[0])) < skip || Math.max(Math.abs(x - wy.xs[wy.n - 1]), Math.abs(z - wy.zs[wy.n - 1])) < skip) {
            wy.pe[i] = target;
            return true;
        }
        int deck = wy.deck[i];
        if (deck == NONE) return false;
        boolean redo = wy.pe[i] > 0, complete = true, bridge = wy.wt[i];
        double R = wy.rad();
        int rc = (int) Math.ceil(R);
        Random r = w.getRandom();
        for (int oz = -rc; oz <= rc; oz++)
            for (int ox = -rc; ox <= rc; ox++) {
                double d = Math.sqrt(ox * ox + oz * oz);
                if (d > R) continue;
                int cx = x + ox, cz = z + oz, own = i;
                double best = d;
                for (int j = Math.max(0, i - 6); j <= Math.min(wy.n - 1, i + 6); j++) {
                    if (j == i) continue;
                    double dj = Math.sqrt((cx - wy.xs[j]) * (cx - wy.xs[j]) + (cz - wy.zs[j]) * (cz - wy.zs[j]));
                    if (dj < best - 1e-9 || Math.abs(dj - best) < 1e-9 && j < own) { best = dj; own = j; }
                }
                if (own != i) continue;
                if (!w.isChunkLoaded(cx >> 4, cz >> 4)) { complete = false; continue; }
                if (Roads.inBuilding(bs, cx, cz)) continue;
                boolean edge = best > wy.lane();
                BlockPos top = new BlockPos(cx, deck, cz);
                if (redo) {
                    // смена облика: только то, что мы же и уложили на проектной высоте
                    if (!Roads.fam(w.getBlockState(top))) continue;
                    if (bridge) {
                        pl.put(top, bridge(era, edge));
                        if (edge && railBlock(w.getBlockState(top.up()))) pl.put(top.up(), rail(era));
                        for (int yy = deck - 1, c = 0; c < 48 && yy > w.getBottomY(); yy--, c++) {
                            BlockPos p = new BlockPos(cx, yy, cz);
                            if (!pillarBlock(w.getBlockState(p))) break;
                            pl.put(p, pillar(era));
                        }
                    } else pl.put(top, wy.narrow ? Roads.mat(era, r) : road(era, edge, i, r));
                    continue;
                }
                int g = TownBuilder.ground(w, cx, cz);
                int sy = g - 1;
                BlockState s = w.getBlockState(new BlockPos(cx, sy, cz));
                if (s.isOf(Blocks.LAVA)) continue;
                if (!s.getFluidState().isEmpty()) {
                    bridge = true;
                    pl.put(top, bridge(era, edge));
                    for (int yy = deck + 1; yy <= deck + 3; yy++) clearUp(w, pl, new BlockPos(cx, yy, cz));
                    if (edge) pl.put(top.up(), rail(era));
                    if (i % 6 == 0 && (edge || d < 0.5) || deck - g > 2 && i % 2 == 0 && edge) pillarDown(w, pl, era, cx, deck - 1, cz);
                    continue;
                }
                if (edge && wy.narrow) continue;
                if (!Roads.terrain(s) && !Roads.fam(s) || s.hasBlockEntity()) continue;
                if (deck - sy > 20 || sy - deck > 14) continue;
                boolean ok = true;
                for (int yy = deck + 1; yy <= sy && ok; yy++) {
                    BlockState u = w.getBlockState(new BlockPos(cx, yy, cz));
                    ok = !u.hasBlockEntity() && (u.isAir() || u.isReplaceable() || Roads.terrain(u) || u.isIn(BlockTags.LEAVES) || u.isIn(BlockTags.LOGS));
                }
                if (!ok) continue;
                for (int yy = sy + 1; yy < deck; yy++) pl.put(new BlockPos(cx, yy, cz), fill(era));
                for (int yy = deck + 1; yy <= sy; yy++) pl.put(new BlockPos(cx, yy, cz), Blocks.AIR);
                pl.put(top, wy.narrow ? Roads.mat(era, r) : road(era, edge, i, r));
                if (deck <= sy) {
                    BlockState below = w.getBlockState(top.down());
                    if (below.isAir() || !below.getFluidState().isEmpty() || below.isReplaceable()) pl.put(top.down(), fill(era));
                }
                for (int yy = deck + 1; yy <= deck + 4; yy++) clearUp(w, pl, new BlockPos(cx, yy, cz));
            }
        if (complete) {
            decor(wy, i, era, pl, bs, redo);
            wy.pe[i] = target;
        }
        return complete;
    }

    /** Фонари, деревья, столбы и постоялый двор; якоря фонарей и столбов запоминаются, чтобы не дублировать и менять стиль. */
    static void decor(Way wy, int i, int era, Place pl, List<Civ.Building> bs, boolean redo) {
        ServerWorld w = wy.w;
        int x = wy.xs[i], z = wy.zs[i], deck = wy.deck[i];
        double px = -wy.tz[i], pz = wy.tx[i];
        if (wy.wt[i]) {
            if (!redo && i % 8 == 4) for (int sd = -1; sd <= 1; sd += 2) {
                int cx = (int) Math.round(x + px * 2 * sd), cz = (int) Math.round(z + pz * 2 * sd);
                if (w.isChunkLoaded(cx >> 4, cz >> 4) && !Roads.inBuilding(bs, cx, cz))
                    pl.put(new BlockPos(cx, deck + 2, cz), (era >= 4 ? era == 9 ? Blocks.SOUL_LANTERN : Blocks.LANTERN : Blocks.TORCH).getDefaultState());
            }
            return;
        }
        if (wy.narrow) {
            if (era >= 2 && i % 16 == 8) anchor(wy, wy.lampY, i, bs, x + px * 2 * ((i / 16) % 2 == 0 ? 1 : -1), z + pz * 2 * ((i / 16) % 2 == 0 ? 1 : -1), redo,
                    (cx, y, cz) -> roadLamp(pl, era, cx, y, cz));
            return;
        }
        if (i % 14 == 0) {
            int sd = (i / 14) % 2 == 0 ? 1 : -1;
            anchor(wy, wy.lampY, i, bs, x + px * 3.5 * sd, z + pz * 3.5 * sd, redo, (cx, y, cz) -> lamp(pl, era, cx, y, cz));
        }
        if (i % 64 == 32) {
            int sd = (i / 64) % 2 == 0 ? 1 : -1;
            anchor(wy, wy.mileY, i, bs, x + px * 3.5 * sd, z + pz * 3.5 * sd, redo, (cx, y, cz) -> milestone(pl, era, wy.k, cx, y, cz));
        }
        if (redo) return;
        if (i % 22 == 11) {
            int sd = (i / 22) % 2 == 0 ? -1 : 1;
            int cx = (int) Math.round(x + px * 5.5 * sd), cz = (int) Math.round(z + pz * 5.5 * sd);
            if (free(wy, bs, cx, cz)) tree(w, pl, wy.k, cx, cz);
        }
        if (!wy.inn && wy.n >= 400 && i >= wy.n / 2) {
            for (int sd = 1; sd >= -1 && !wy.inn; sd -= 2) {
                int cx = (int) Math.round(x + px * 9 * sd), cz = (int) Math.round(z + pz * 9 * sd);
                if (!w.isChunkLoaded(cx >> 4, cz >> 4) || !TownBuilder.dryArea(w, cx, cz, 4)) continue;
                boolean clash = false;
                for (int dx = -4; dx <= 4 && !clash; dx += 2) for (int dz = -4; dz <= 4; dz += 2) if (Roads.inBuilding(bs, cx + dx, cz + dz) || corridor(wy).contains(BlockPos.asLong(cx + dx, 0, cz + dz))) { clash = true; break; }
                if (clash) continue;
                inn(wy, pl, era, cx, cz, -px * sd, -pz * sd);
                wy.inn = true;
            }
            if (i > wy.n / 2 + 120) wy.inn = true;
        }
    }

    private interface Put { void at(int x, int y, int z); }

    /** Ставит предмет декора в слоте i: по якорю (перекладка) или на свободное место рядом с дорогой (запоминает высоту). */
    private static void anchor(Way wy, Map<Integer, Integer> map, int i, List<Civ.Building> bs, double fx, double fz, boolean redo, Put put) {
        int cx = (int) Math.round(fx), cz = (int) Math.round(fz);
        Integer y = map.get(i);
        if (y != null) {
            if (redo && wy.w.isChunkLoaded(cx >> 4, cz >> 4)) put.at(cx, y, cz);
            return;
        }
        if (!free(wy, bs, cx, cz)) return;
        int gy = TownBuilder.ground(wy.w, cx, cz);
        if (Math.abs(gy - wy.deck[i]) > 2) return;
        map.put(i, gy);
        put.at(cx, gy, cz);
    }

    static void lamp(Place pl, int era, int x, int y, int z) {
        BlockPos b = new BlockPos(x, y, z);
        Block post, light;
        int h;
        if (era <= 1) { post = Blocks.OAK_FENCE; light = Blocks.TORCH; h = 1; }
        else if (era <= 3) { post = Blocks.SPRUCE_FENCE; light = Blocks.LANTERN; h = 2; }
        else if (era <= 5) { post = Blocks.COBBLESTONE_WALL; light = Blocks.LANTERN; h = 2; }
        else if (era <= 8) { post = Blocks.STONE_BRICK_WALL; light = Blocks.LANTERN; h = 3; }
        else if (era == 9) { post = Blocks.DEEPSLATE_BRICK_WALL; light = Blocks.SOUL_LANTERN; h = 3; }
        else { post = Blocks.POLISHED_BLACKSTONE_WALL; light = Blocks.LANTERN; h = 3; }
        if (era >= 6) { pl.put(b, era >= 10 ? Blocks.WAXED_CUT_COPPER : Blocks.POLISHED_ANDESITE); b = b.up(); h--; }
        for (int k = 0; k < h; k++) pl.put(b.up(k), post);
        pl.put(b.up(h), light);
        if (era >= 9) pl.put(b.up(h + 1), era == 9 ? Blocks.AMETHYST_CLUSTER : Blocks.LIGHTNING_ROD);
    }

    /** Придорожный столбик узкой дороги: забор с факелом, с эпохи 4 — стена с фонарём. */
    static void roadLamp(Place pl, int era, int x, int y, int z) {
        pl.put(new BlockPos(x, y, z), era >= 4 ? Blocks.STONE_BRICK_WALL : Blocks.OAK_FENCE);
        pl.put(new BlockPos(x, y + 1, z), era >= 4 ? Blocks.LANTERN : Blocks.TORCH);
    }

    static void milestone(Place pl, int era, Kingdoms.K k, int x, int y, int z) {
        BlockPos b = new BlockPos(x, y, z);
        Block base = era < 4 ? Blocks.COBBLESTONE : era < 9 ? Blocks.CHISELED_STONE_BRICKS : era == 9 ? Blocks.CHISELED_DEEPSLATE : Blocks.CHISELED_POLISHED_BLACKSTONE;
        pl.put(b, base);
        pl.put(b.up(), banner(k));
    }

    static void tree(ServerWorld w, Place pl, Kingdoms.K k, int x, int z) {
        int g = TownBuilder.ground(w, x, z);
        if (!w.getBlockState(new BlockPos(x, g - 1, z)).isIn(BlockTags.DIRT)) return;
        for (int h = 0; h < 6; h++) {
            BlockState s = w.getBlockState(new BlockPos(x, g + h, z));
            if (!s.isAir() && !s.isReplaceable()) return;
        }
        Block log, leaf;
        switch (k) {
            case RED -> { log = Blocks.CHERRY_LOG; leaf = Blocks.CHERRY_LEAVES; }
            case BLUE -> { log = Blocks.SPRUCE_LOG; leaf = Blocks.SPRUCE_LEAVES; }
            case GREEN -> { log = Blocks.OAK_LOG; leaf = Blocks.OAK_LEAVES; }
            case ORANGE, BROWN -> { log = Blocks.ACACIA_LOG; leaf = Blocks.ACACIA_LEAVES; }
            case PURPLE, BLACK -> { log = Blocks.DARK_OAK_LOG; leaf = Blocks.DARK_OAK_LEAVES; }
            case CYAN, LIGHT_BLUE -> { log = Blocks.JUNGLE_LOG; leaf = Blocks.JUNGLE_LEAVES; }
            case PINK -> { log = Blocks.CHERRY_LOG; leaf = Blocks.FLOWERING_AZALEA_LEAVES; }
            default -> { log = Blocks.BIRCH_LOG; leaf = Blocks.BIRCH_LEAVES; }
        }
        BlockState lv = leaf.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int h = 0; h < 4; h++) pl.put(new BlockPos(x, g + h, z), log);
        for (int y = g + 2; y <= g + 5; y++) {
            int rad = y <= g + 3 ? 2 : 1;
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.abs(dx) == rad && Math.abs(dz) == rad && rad > 1) continue;
                    if (y == g + 5 && Math.abs(dx) + Math.abs(dz) > 1) continue;
                    if (dx == 0 && dz == 0 && y < g + 4) continue;
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    if (w.getBlockState(p).isAir()) pl.put(p, lv);
                }
        }
    }

    static void inn(Way wy, Place pl, int era, int cx, int cz, double ux, double uz) {
        ServerWorld w = wy.w;
        int gy = TownBuilder.ground(w, cx, cz);
        int[][] doors = {{0, -3}, {0, 3}, {-3, 0}, {3, 0}};
        int[] door = doors[0];
        double best = -9;
        for (int[] d : doors) {
            double v = d[0] * ux + d[1] * uz;
            if (v > best) { best = v; door = d; }
        }
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int yy = gy - 2; yy >= gy - 5; yy--) {
                    BlockPos p = new BlockPos(x, yy, z);
                    BlockState st = w.getBlockState(p);
                    if (st.isAir() || st.isReplaceable()) pl.put(p, st(era, Blocks.COBBLESTONE));
                }
                boolean in = Math.abs(dx) < 3 && Math.abs(dz) < 3;
                pl.put(new BlockPos(x, gy - 1, z), in ? st(era, Blocks.SPRUCE_PLANKS) : st(era, Blocks.COBBLESTONE));
                for (int h = 0; h <= 6; h++) pl.put(new BlockPos(x, gy + h, z), Blocks.AIR.getDefaultState());
                boolean wall = !in, corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                if (wall) for (int h = 0; h < 4; h++) {
                    BlockPos p = new BlockPos(x, gy + h, z);
                    boolean isDoor = dx == door[0] && dz == door[1] && h < 2;
                    if (isDoor) continue;
                    boolean window = !corner && h == 1 && (dx == 0 || dz == 0 || Math.abs(dx) == 1 && Math.abs(dz) == 3 || Math.abs(dz) == 1 && Math.abs(dx) == 3);
                    pl.put(p, corner ? st(era, Blocks.OAK_LOG) : window ? st(era, Blocks.GLASS_PANE) : st(era, Blocks.OAK_PLANKS));
                }
                pl.put(new BlockPos(x, gy + 4, z), st(era, Blocks.DARK_OAK_PLANKS));
                if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) pl.put(new BlockPos(x, gy + 5, z), st(era, Blocks.DARK_OAK_PLANKS));
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) pl.put(new BlockPos(x, gy + 6, z), st(era, Blocks.DARK_OAK_SLAB));
            }
        pl.put(new BlockPos(cx + 2, gy, cz + 2), Blocks.LANTERN);
        pl.put(new BlockPos(cx - 2, gy, cz - 2), Blocks.BARREL);
        pl.put(new BlockPos(cx - 2, gy, cz + 2), Blocks.CRAFTING_TABLE);
        pl.put(new BlockPos(cx + 2, gy, cz - 2), Blocks.SMOKER);
        pl.put(new BlockPos(cx - 2, gy, cz), Blocks.WHITE_BED);
        pl.put(new BlockPos(cx + 2, gy + 6, cz - 2), Blocks.CAMPFIRE);
        pl.put(new BlockPos(cx + door[0] + (int) Math.signum(door[0]), gy, cz + door[1] + (int) Math.signum(door[1])), Blocks.AIR);
        int hx = cx + door[0] + (door[1] != 0 ? 2 : (int) Math.signum(door[0])), hz = cz + door[1] + (door[0] != 0 ? 2 : (int) Math.signum(door[1]));
        pl.put(new BlockPos(hx, gy, hz), Blocks.HAY_BLOCK);
        pl.put(new BlockPos(hx, gy + 1, hz), Blocks.LANTERN);
        Chronicle.say(w.getServer(), wy.k, "открывает постоялый двор на дороге «" + wy.name + "»");
    }

    private static BlockState st(int era, Block b) { return Eras.style(era, Civ.Kind.HOUSE, b.getDefaultState()); }

    // ---------------- строительство ----------------
    static ServerWorld world(Way wy) {
        if (wy.w == null && wy.k.world != null) wy.w = wy.k.world;
        return wy.w;
    }

    static void step(Way wy) {
        if (wy.last != null && !wy.last.finished()) return;
        int end = Math.min(wy.n, wy.i + BATCH);
        ensureDeck(wy, wy.i, end);
        Place pl = new Place(wy.w);
        List<Civ.Building> bs = allBuildings();
        for (; wy.i < end; wy.i++) point(wy, wy.i, wy.era, pl, bs);
        if (!pl.ps.isEmpty()) { Tasks.add(pl); wy.last = pl; }
    }

    /** Добивает точки, пропущенные из-за незагруженных чанков (раньше они бросались после 15 попыток). */
    static void retry(Way wy) {
        if (wy.last != null && !wy.last.finished()) return;
        Place pl = new Place(wy.w);
        List<Civ.Building> bs = null;
        int tgt = wy.era + 1, done = 0;
        for (int j = 0; j < wy.n && done < 60; j++) {
            if (wy.pe[j] >= tgt || !wy.w.isChunkLoaded(wy.xs[j] >> 4, wy.zs[j] >> 4)) continue;
            if (wy.deck[j] == NONE) ensureDeck(wy, j, j + BATCH);
            if (bs == null) bs = allBuildings();
            point(wy, j, wy.era, pl, bs);
            done++;
        }
        if (!pl.ps.isEmpty()) { Tasks.add(pl); wy.last = pl; }
    }

    static boolean active(String key) {
        for (Way wy : WAYS) if (wy.key.equals(key)) return true;
        return false;
    }

    static boolean busy(Kingdoms.K k) {
        for (Way wy : WAYS) if (wy.k == k && !wy.narrow && !wy.fin) return true;
        return false;
    }

    /** Забывает готовую трассу по ключу (перестройка по воле бога). */
    static void forget(String key) {
        WAYS.removeIf(wy -> wy.key.equals(key) && wy.fin);
    }

    static boolean start(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b, boolean narrow) {
        String key = Roads.key(a, b);
        if (active(key)) return false;
        Way wy = plan(w, k, a, b, key, narrow);
        if (wy == null) return false;
        WAYS.add(wy);
        if (!narrow) Chronicle.say(w.getServer(), k, "начинает прокладывать «" + wy.name + "» (" + (int) Math.sqrt(a.getSquaredDistance(b)) + " блоков)");
        return true;
    }

    static void finish(MinecraftServer s, Way wy) {
        if (wy.narrow) return;
        DONE.add(wy.key);
        BUILT.merge(wy.k, 1, Integer::sum);
        Chronicle.say(s, wy.k, "завершает «" + wy.name + "»");
    }

    static BlockPos[] choose(Kingdoms.K k) {
        List<BlockPos> nodes = new ArrayList<>();
        nodes.add(k.capital);
        nodes.addAll(Conquest.towns(k));
        BlockPos[] best = null;
        double bd = Double.MAX_VALUE;
        for (int a = 1; a < nodes.size(); a++) {
            BlockPos p = nodes.get(a), q = null;
            double qd = Double.MAX_VALUE;
            for (int b = 0; b < nodes.size(); b++) {
                if (b == a) continue;
                double d = p.getSquaredDistance(nodes.get(b));
                if (d < qd) { qd = d; q = nodes.get(b); }
            }
            for (BlockPos o : new BlockPos[]{q, k.capital}) {
                if (o == null || o.equals(p)) continue;
                double d = p.getSquaredDistance(o);
                if (d < 100 * 100 || d > 2000.0 * 2000.0) continue;
                String key = Roads.key(o, p);
                if (DONE.contains(key) || Roads.DONE.contains(key) || active(key)) continue;
                if (d < bd) { bd = d; best = new BlockPos[]{o, p}; }
            }
        }
        if (best != null) return best;
        for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o == k || !o.alive || o.capital == null || o.world != k.world) continue;
            if (Diplomacy.rel(k, o) != Diplomacy.ALLY && Vassals.lord(k) != o) continue;
            double d = k.capital.getSquaredDistance(o.capital);
            if (d < 100 * 100 || d > 2000.0 * 2000.0) continue;
            String key = Roads.key(k.capital, o.capital);
            if (DONE.contains(key) || Roads.DONE.contains(key) || active(key)) continue;
            return new BlockPos[]{k.capital, o.capital};
        }
        return null;
    }

    public static void tick(MinecraftServer s) {
        ++t;
        if (t % 10 == 0) for (Way wy : new ArrayList<>(WAYS)) {
            if (!wy.k.alive) { if (!wy.fin) WAYS.remove(wy); continue; }
            if (world(wy) == null) continue;
            if (wy.i < wy.n) step(wy);
            else if (!wy.done) {
                wy.done = true;
                wy.corr = null;
                if (!wy.fin) { wy.fin = true; finish(s, wy); }
            } else if (t % 200 == 0) { retry(wy); wy.corr = null; }
        }
        if (t % 600 == 0) for (Way wy : WAYS) {
            // новая эпоха: вся трасса (полотно, кромки, перила, опоры, фонари) перекладывается заново
            if (wy.done && wy.k.alive && wy.k.world != null && Civ.city(wy.k).era != wy.era) {
                wy.era = Civ.city(wy.k).era;
                wy.i = 0;
                wy.done = false;
            }
        }
        if (!on || t % 3000 != 0) return;
        for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive || k.capital == null || !(k.world instanceof ServerWorld w)) continue;
            Civ.City c = Civ.city(k);
            if (c.era < 2 && !Tech.has(k, Tech.T.WHEEL)) continue;
            if (c.stone < 20 || c.wood < 10 || busy(k)) continue;
            BlockPos[] pair = choose(k);
            if (pair == null) continue;
            if (start(w, k, pair[0], pair[1], false)) { c.stone -= 20; c.wood -= 10; }
        }
    }

    public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
        Kingdoms.K k = Tech.nearest(w, pos);
        if (k == null || k.capital == null) {
            god.sendMessage(Text.literal("Рядом нет королевства").formatted(Formatting.GRAY), true);
            return;
        }
        BlockPos to = TownBuilder.landOr(w, Fx.surface(w, pos.getX(), pos.getZ()));
        if (k.capital.getSquaredDistance(to) < 30 * 30) {
            god.sendMessage(Text.literal("Слишком близко к столице — укажи точку подальше").formatted(Formatting.GRAY), true);
            return;
        }
        String key = Roads.key(k.capital, to);
        DONE.remove(key);
        forget(key);
        if (start(w, k, k.capital, to, false)) god.sendMessage(Text.literal("🛣 " + k.title + ": тракт прокладывается сюда").formatted(k.color), true);
        else god.sendMessage(Text.literal("Этот тракт уже строится или слишком длинный").formatted(Formatting.GRAY), true);
    }

    static int count(Kingdoms.K k) { return BUILT.getOrDefault(k, 0); }

    public static void clear() {
        DONE.clear();
        WAYS.clear();
        BUILT.clear();
        t = 0;
    }

    // ---------------- сохранение трасс (@roadnet) ----------------
    private static String enc(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int v : a) sb.append(v == NONE ? "--" : String.format("%2s", Integer.toString(v + 64, 36)).replace(' ', '0'));
        return sb.toString();
    }

    private static String encMap(Map<Integer, Integer> m) {
        if (m.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (var e : m.entrySet()) sb.append(sb.length() == 0 ? "" : ";").append(e.getKey()).append(':').append(e.getValue());
        return sb.toString();
    }

    private static void decMap(String s, Map<Integer, Integer> m) {
        if (s.equals("-") || s.isEmpty()) return;
        for (String p : s.split(";")) {
            String[] kv = p.split(":");
            m.put(Integer.parseInt(kv[0]), Integer.parseInt(kv[1]));
        }
    }

    static List<String> saveLines() {
        List<String> out = new ArrayList<>();
        for (Way wy : WAYS) {
            StringBuilder path = new StringBuilder(wy.xs[0] + "," + wy.zs[0] + ",");
            for (int j = 1; j < wy.n; j++) path.append((wy.xs[j] - wy.xs[j - 1] + 1) * 3 + (wy.zs[j] - wy.zs[j - 1] + 1));
            StringBuilder pe = new StringBuilder(), wt = new StringBuilder();
            for (int j = 0; j < wy.n; j++) {
                pe.append(Character.forDigit(wy.pe[j], 36));
                wt.append(wy.wt[j] ? '1' : '0');
            }
            out.add("W|" + wy.key + "|" + wy.name.replace('|', '/') + "|" + wy.k.name() + "|" + (wy.narrow ? 1 : 0) + "|" + wy.era + "|"
                    + (wy.done ? 1 : 0) + (wy.fin ? 1 : 0) + (wy.inn ? 1 : 0) + "|" + wy.i + "|" + path + "|" + enc(wy.deck) + "|" + pe + "|" + wt
                    + "|" + encMap(wy.lampY) + "|" + encMap(wy.mileY));
        }
        return out;
    }

    static void loadLine(String[] a) {
        try {
            if (a.length < 14 || !a[0].equals("W")) return;
            String[] head = a[8].split(",", 3);
            int x = Integer.parseInt(head[0]), z = Integer.parseInt(head[1]);
            List<int[]> pts = new ArrayList<>();
            pts.add(new int[]{x, z});
            for (char ch : head[2].toCharArray()) {
                int d = ch - '0';
                x += d / 3 - 1;
                z += d % 3 - 1;
                pts.add(new int[]{x, z});
            }
            Way wy = new Way(Kingdoms.K.valueOf(a[3]), null, a[1], a[2], a[4].equals("1"), pts, Integer.parseInt(a[5]));
            if (a[9].length() != wy.n * 2 || a[10].length() != wy.n || a[11].length() != wy.n) return;
            wy.done = a[6].charAt(0) == '1';
            wy.fin = a[6].charAt(1) == '1';
            wy.inn = a[6].charAt(2) == '1';
            wy.i = Math.min(wy.n, Integer.parseInt(a[7]));
            for (int j = 0; j < wy.n; j++) {
                String c = a[9].substring(2 * j, 2 * j + 2);
                wy.deck[j] = c.equals("--") ? NONE : Integer.parseInt(c, 36) - 64;
                wy.pe[j] = (byte) Character.digit(a[10].charAt(j), 36);
                wy.wt[j] = a[11].charAt(j) == '1';
            }
            decMap(a[12], wy.lampY);
            decMap(a[13], wy.mileY);
            WAYS.add(wy);
        } catch (RuntimeException e) {
            GodBox.LOG.warn("GodBox: трасса не прочитана из сохранения: {}", e.toString());
        }
    }
}
