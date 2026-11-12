package com.vladik.godbox;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Малые постройки не-«Building»: колодцы, прилавки, фонари города и портовые причалы.
 * Реестр помнит, что где стоит и в какой эпохе построено; чертёж {@link #build} — единственный источник
 * истины и для первой постройки, и для перекраски (Eras.tick подтягивает эпоху). Старые сохранения этих
 * построек не содержат, поэтому уже стоящие колодцы и причалы без записи не перекрашиваются.
 */
final class Props {
    private Props() {}

    enum Kind { WELL, STALL, LAMP, PIER, MARKET }

    static final class Prop {
        final Kind kind;
        Kingdoms.K k;
        final int x, y, z;
        /** STALL: зерно рисунка; PIER: горизонтальный индекс направления на сушу. */
        final int arg;
        /** Эпоха, в которой постройка реально стоит; -1 = нужно перекрасить. */
        int era;
        /** Ещё выкладывается: перекрашивать нельзя. */
        boolean busy;
        long retryAt;

        Prop(Kind kind, Kingdoms.K k, int x, int y, int z, int arg, int era) {
            this.kind = kind;
            this.k = k;
            this.x = x;
            this.y = y;
            this.z = z;
            this.arg = arg;
            this.era = era;
        }
    }

    private static final List<Prop> ALL = new ArrayList<>();

    static void init() {
        SaveExt.register("prop", Props::save, Props::load, ALL::clear);
    }

    static void clear() { ALL.clear(); }

    /** Записывает постройку (повторная запись на том же месте заменяет прежнюю). */
    static Prop add(Kind kind, Kingdoms.K k, int x, int y, int z, int arg, int era) {
        ALL.removeIf(p -> p.kind == kind && p.x == x && p.y == y && p.z == z);
        Prop p = new Prop(kind, k, x, y, z, arg, era);
        p.busy = true;
        ALL.add(p);
        return p;
    }

    static boolean has(Prop p) { return ALL.contains(p); }

    static List<Prop> of(Kingdoms.K k) {
        List<Prop> out = new ArrayList<>();
        for (Prop p : ALL) if (p.k == k) out.add(p);
        return out;
    }

    /** Смена эпохи или расы: все готовые постройки королевства требуют перекраски. */
    static void markStale(Kingdoms.K k) {
        for (Prop p : ALL) {
            if (p.k != k || p.busy) continue;
            p.era = -1;
            p.retryAt = 0;
        }
    }

    /** Постройки павшего королевства достаются тому, кто владеет ближайшим городом (или столицей рядом с причалом). */
    static void adopt(ServerWorld w) {
        for (Prop p : ALL) {
            if (p.k != null && p.k.alive && p.k.world == w) continue;
            Kingdoms.K best = null;
            double bd = Double.MAX_VALUE;
            for (Kingdoms.K o : Kingdoms.K.values()) {
                if (!o.alive || o.world != w || o.capital == null) continue;
                double d = Math.min(dist(o.capital, p), nearestTown(o, p));
                if (d < bd) {
                    bd = d;
                    best = o;
                }
            }
            if (best != null && bd < 80 * 80) {
                if (p.k != best) {
                    p.era = -1;
                    p.retryAt = 0;
                }
                p.k = best;
            }
        }
    }

    /** Город взят: постройки вокруг него переходят захватчику и перекрашиваются в его эпоху и цвета. */
    static void transfer(Kingdoms.K from, Kingdoms.K to, BlockPos town, int r) {
        for (Prop p : ALL) {
            if (p.k != from) continue;
            double dx = p.x - town.getX(), dz = p.z - town.getZ();
            if (dx * dx + dz * dz > r * r) continue;
            p.k = to;
            p.era = -1;
            p.retryAt = 0;
        }
    }

    private static double dist(BlockPos c, Prop p) {
        double dx = c.getX() - p.x, dz = c.getZ() - p.z;
        return dx * dx + dz * dz;
    }

    private static double nearestTown(Kingdoms.K k, Prop p) {
        double best = Double.MAX_VALUE;
        List<BlockPos> ts = Conquest.TOWNS.get(k);
        if (ts != null) for (BlockPos t : new ArrayList<>(ts)) best = Math.min(best, dist(t, p));
        return best;
    }

    /** Чертёж постройки в эпохе era: сам по себе ничего не читает из мира, кроме земли под фундаментом. */
    static TownBuilder.Job blueprint(ServerWorld w, Prop p, int era) {
        TownBuilder.Job j = new TownBuilder.Job(w);
        j.dry = true;
        build(j, p, era);
        return j;
    }

    static void build(TownBuilder.Job j, Prop p, int era) {
        switch (p.kind) {
            case WELL -> TownBuilder.well(j, era, p.x, p.z, p.y);
            case STALL -> TownBuilder.stall(j, p.k, p.x, p.z, p.y, p.arg);
            case LAMP -> TownBuilder.lamp(j, era, p.x, p.y, p.z);
            case PIER -> Ships.pier(j, p.k, era, new BlockPos(p.x, p.y, p.z), Direction.fromHorizontal(p.arg));
            case MARKET -> market(j, p, era);
        }
    }

    /** Рыночная площадь 7x7 с полосатым навесом цвета королевства; центр — (p.x, p.y, p.z). */
    private static void market(TownBuilder.Job j, Prop p, int era) {
        Block floor = Eras.style(era, Civ.Kind.HOUSE, Blocks.STONE_BRICKS.getDefaultState()).getBlock();
        Block post = Eras.style(era, Civ.Kind.HOUSE, Blocks.OAK_FENCE.getDefaultState()).getBlock();
        int x = p.x, y = p.y, z = p.z;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                j.put(new BlockPos(x + dx, y - 1, z + dz), floor.getDefaultState());
                for (int h = 0; h <= 4; h++) j.put(new BlockPos(x + dx, y + h, z + dz), Blocks.AIR.getDefaultState());
                if (Math.abs(dx) >= 1) j.put(new BlockPos(x + dx, y + 3, z + dz), ((dz & 1) == 0 ? p.k.wool : Blocks.WHITE_WOOL).getDefaultState());
            }
        for (int px : new int[]{-3, -1, 1, 3})
            for (int pz : new int[]{-3, 3})
                for (int h = 0; h < 3; h++) j.put(new BlockPos(x + px, y + h, z + pz), post.getDefaultState());
        j.put(new BlockPos(x - 2, y, z - 2), Blocks.BARREL.getDefaultState());
        j.put(new BlockPos(x - 2, y, z + 1), Blocks.HAY_BLOCK.getDefaultState());
        j.put(new BlockPos(x + 2, y, z - 1), Blocks.BARREL.getDefaultState());
        j.put(new BlockPos(x + 2, y, z + 2), Blocks.COMPOSTER.getDefaultState());
        j.put(new BlockPos(x + 2, y, z), Blocks.SMITHING_TABLE.getDefaultState());
        j.put(new BlockPos(x - 2, y, z), Blocks.LOOM.getDefaultState());
        j.put(new BlockPos(x, y, z), Blocks.BELL.getDefaultState());
        j.put(new BlockPos(x, y + 4, z), Blocks.LANTERN.getDefaultState());
    }

    private static List<String> save() {
        List<String> out = new ArrayList<>();
        for (Prop p : ALL) {
            if (p.busy || p.k == null) continue;
            out.add(p.kind.name() + "|" + p.k.name() + "|" + p.x + "|" + p.y + "|" + p.z + "|" + p.arg + "|" + p.era);
        }
        return out;
    }

    private static void load(String[] a) {
        if (a.length < 7) return;
        try {
            ALL.add(new Prop(Kind.valueOf(a[0]), Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]),
                    Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6])));
        } catch (IllegalArgumentException e) {
            GodBox.LOG.warn("GodBox: запись постройки пропущена: {}", String.join("|", a));
        }
    }
}
