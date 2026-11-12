package com.vladik.godbox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ServerWorldProperties;

/**
 * v3.2: видимые изменения мира по сезонам (снег, лёд, цветы, тыквы, грибы), погода и праздник урожая.
 * Всё делается выборочно: каждый тик берётся небольшой запас случайных колонок рядом с игроками
 * и столицами, чанки не грузятся, блок-сущности не трогаются.
 */
final class SeasonWorld {
    /** Пока true, WorldMixin не пишет изменения в историю отмены бога. */
    static boolean quiet;

    private static final int THAW_TICKS = 12000;
    private static int thaw, t, hold;
    private static boolean lastOn = true, inited, festival;
    private static boolean wasRaining;
    private static int lastTimer;
    private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER};

    private SeasonWorld() {}

    static boolean worldOn() { return Config.b("seasons.world", true); }

    static void reset() {
        thaw = 0;
        t = 0;
        hold = 0;
        festival = false;
        inited = false;
    }

    // ---------------- тик ----------------
    static void tick(MinecraftServer s) {
        if (!inited) {
            inited = true;
            lastOn = Seasons.on;
        }
        if (lastOn && !Seasons.on) thaw = THAW_TICKS;
        lastOn = Seasons.on;
        t++;
        if (Seasons.on && t % 20 == 0) {
            if (Config.b("seasons.weather", true)) weather(s);
            if (Config.b("seasons.festival", true)) festival(s);
        }
        if (!Seasons.on) {
            if (thaw <= 0) return;
            thaw--;
        } else if (!worldOn()) {
            return;
        }
        if (!Perf.go) return;
        int len = Seasons.length();
        int budget = Math.max(8, Math.min(48, Math.round(16f * 6000f / len)));
        if (Perf.slow) budget /= 2;
        int left = 64;
        for (ServerWorld w : s.getWorlds()) {
            if (w.getPlayers().isEmpty() || !w.getDimension().hasSkyLight() || w.getDimension().hasCeiling()) continue;
            Random r = w.getRandom();
            for (ServerPlayerEntity p : w.getPlayers()) {
                int n = Math.min(budget, left);
                left -= n;
                BlockPos c = p.getBlockPos();
                for (int i = 0; i < n; i++) sample(w, r, c.getX() + r.nextInt(113) - 56, c.getZ() + r.nextInt(113) - 56);
            }
            if (t % 2 == 0) {
                for (Kingdoms.K k : Kingdoms.K.values()) {
                    if (!k.alive || k.world != w || k.capital == null || left <= 0) continue;
                    for (int i = 0; i < 3; i++) sample(w, r, k.capital.getX() + r.nextInt(73) - 36, k.capital.getZ() + r.nextInt(73) - 36);
                    left -= 3;
                }
            }
        }
    }

    // ---------------- выборка одной колонки ----------------
    private static boolean winterish(int se, float p, boolean cold) {
        if (se == Seasons.WINTER) return true;
        return cold && ((se == Seasons.AUTUMN && p > 0.7f) || (se == Seasons.SPRING && p < 0.3f));
    }

    /** Предел слоёв снега для колонки (0 — снегу тут быть не должно). */
    private static int cap(boolean natural, boolean cold, boolean hot, boolean act) {
        if (hot) return 0;
        int base = natural ? 1 : 0;
        if (!act) return base;
        int se = Seasons.index();
        float p = Seasons.progress();
        if (!winterish(se, p, cold)) return base;
        if (se == Seasons.WINTER) return Math.max(base, Math.min(cold ? 4 : 3, 1 + (int) (p * 4)));
        return Math.max(base, se == Seasons.SPRING && p < 0.15f ? 2 : 1);
    }

    private static void sample(ServerWorld w, Random r, int x, int z) {
        if (w.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) return;
        BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
        if (w.isOutOfHeightLimit(top.getY() - 1)) return;
        BlockPos base = top.down();
        BlockState above = w.getBlockState(top);
        BlockState bs = w.getBlockState(base);
        Biome biome = w.getBiome(top).value();
        float temp = biome.getTemperature();
        boolean act = Seasons.on;
        boolean hot = temp >= 0.9f || !biome.hasPrecipitation();
        boolean cold = temp < 0.3f;
        boolean natural = biome.getPrecipitation(top) == Biome.Precipitation.SNOW;
        int se = Seasons.index();
        float p = Seasons.progress();
        boolean wet = w.isRaining();

        quiet = true;
        try {
            // снег
            int cap = cap(natural, cold, hot, act);
            if (bs.isOf(Blocks.SNOW)) {
                int layers = bs.get(SnowBlock.LAYERS);
                if (layers > cap) {
                    if (r.nextInt(100) < (act ? 55 : 80)) {
                        if (layers == 1) w.setBlockState(base, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        else w.setBlockState(base, bs.with(SnowBlock.LAYERS, layers - 1), Block.NOTIFY_ALL);
                    }
                } else if (layers < cap && layers < 8 && r.nextInt(100) < (wet ? 60 : 20)) {
                    w.setBlockState(base, bs.with(SnowBlock.LAYERS, layers + 1), Block.NOTIFY_ALL);
                }
                return;
            }
            if (cap > 0 && act && above.isAir() && !bs.isOf(Blocks.WATER) && r.nextInt(100) < (wet ? 60 : 20)
                    && w.getLightLevel(LightType.BLOCK, top) < 10 && Blocks.SNOW.getDefaultState().canPlaceAt(w, top)) {
                w.setBlockState(top, Blocks.SNOW.getDefaultState(), Block.NOTIFY_ALL);
                return;
            }
            // лёд на воде
            if (bs.isOf(Blocks.WATER) && !natural) {
                if (act && !hot && above.isAir() && winterish(se, p, cold) && (se != Seasons.WINTER || cold || p > 0.1f)
                        && bs.get(FluidBlock.LEVEL) == 0 && r.nextInt(100) < 35 && freezeOk(w, base, top)) {
                    w.setBlockState(base, Blocks.ICE.getDefaultState(), Block.NOTIFY_ALL);
                }
                return;
            }
            if (bs.isOf(Blocks.ICE) && !natural && w.getBlockState(base.down()).isOf(Blocks.WATER) && above.isAir()
                    && (!act || !winterish(se, p, cold)) && r.nextInt(100) < (act ? 45 : 80)) {
                w.setBlockState(base, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
                return;
            }
            if (!act || hot || !above.isAir() || !bs.isOf(Blocks.GRASS_BLOCK)) {
                if (act && !hot && se == Seasons.AUTUMN && r.nextInt(25) == 0) mushroom(w, r, x, z, top);
                return;
            }
            // весна и осень: цветы, трава, тыквы
            if (se == Seasons.SPRING && p > 0.1f && r.nextInt(4) == 0) {
                if (r.nextInt(5) < 3) {
                    if (density(w, r, top, 7, s -> s.isOf(Blocks.SHORT_GRASS)) <= 2) w.setBlockState(top, Blocks.SHORT_GRASS.getDefaultState(), Block.NOTIFY_ALL);
                } else if (density(w, r, top, 7, s -> s.isIn(BlockTags.SMALL_FLOWERS)) <= 1) {
                    w.setBlockState(top, FLOWERS[r.nextInt(FLOWERS.length)].getDefaultState(), Block.NOTIFY_ALL);
                }
            } else if (se == Seasons.AUTUMN && !cold && r.nextInt(60) == 0 && farmNear(w, top)
                    && density(w, r, top, 10, s -> s.isOf(Blocks.PUMPKIN)) == 0) {
                w.setBlockState(top, Blocks.PUMPKIN.getDefaultState(), Block.NOTIFY_ALL);
            } else if (se == Seasons.AUTUMN) {
                if (r.nextInt(25) == 0) mushroom(w, r, x, z, top);
            }
        } finally {
            quiet = false;
        }
    }

    private static boolean farmNear(ServerWorld w, BlockPos p) {
        for (int i = 0; i < 4; i++) {
            Direction d = Direction.Type.HORIZONTAL.random(w.getRandom());
            for (int n = 2; n <= 5; n += 3) {
                BlockState s = w.getBlockState(p.offset(d, n).down());
                if (s.isOf(Blocks.FARMLAND) || s.isOf(Blocks.HAY_BLOCK)) return true;
            }
        }
        return false;
    }

    /** Грибы под кронами деревьев; ставятся только в тени и редко. */
    private static void mushroom(ServerWorld w, Random r, int x, int z, BlockPos canopy) {
        BlockPos g = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (g.getY() >= canopy.getY() - 1 || !w.getBlockState(g).isAir()) return;
        Block m = r.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
        if (!m.getDefaultState().canPlaceAt(w, g)) return;
        if (density(w, r, g, 8, s -> s.isOf(Blocks.BROWN_MUSHROOM) || s.isOf(Blocks.RED_MUSHROOM)) > 1) return;
        w.setBlockState(g, m.getDefaultState(), Block.NOTIFY_ALL);
    }

    /** Сколько блоков, подходящих под условие, нашлось среди n случайных точек рядом: так плотность растений ограничена. */
    private static int density(ServerWorld w, Random r, BlockPos c, int n, java.util.function.Predicate<BlockState> f) {
        int cnt = 0;
        for (int i = 0; i < n; i++) {
            if (f.test(w.getBlockState(c.add(r.nextInt(9) - 4, 0, r.nextInt(9) - 4)))) cnt++;
        }
        return cnt;
    }

    /** Замерзает только вода у берега или рядом со льдом, не у портов, где ходят корабли, и не у огней. */
    private static boolean freezeOk(ServerWorld w, BlockPos water, BlockPos air) {
        if (w.getLightLevel(LightType.BLOCK, air) >= 10) return false;
        boolean edge = false;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockState n = w.getBlockState(water.offset(d));
            if (n.isOf(Blocks.ICE) || (!n.isOf(Blocks.WATER) && !n.isAir() && !n.isOf(Blocks.KELP) && !n.isOf(Blocks.SEAGRASS))) edge = true;
        }
        if (!edge) return false;
        for (var e : Ships.PORTS.entrySet()) {
            BlockPos q = e.getValue();
            if (q == null || e.getKey().world != w || Seasons.frozen(e.getKey())) continue;
            int dx = q.getX() - water.getX(), dz = q.getZ() - water.getZ();
            if (dx * dx + dz * dz < 48 * 48) return false;
        }
        return true;
    }

    // ---------------- погода ----------------
    /** Ясного/дождливого времени в долях сезона: {ясно, дождь}. */
    private static float[] frac(int se) {
        return switch (se) {
            case Seasons.SPRING -> new float[]{0.30f, 0.25f};
            case Seasons.SUMMER -> new float[]{0.80f, 0.10f};
            case Seasons.AUTUMN -> new float[]{0.35f, 0.22f};
            default -> new float[]{0.25f, 0.30f};
        };
    }

    private static float thunderChance(int se) {
        return switch (se) {
            case Seasons.SUMMER -> 0.5f;
            case Seasons.SPRING -> 0.2f;
            case Seasons.AUTUMN -> 0.1f;
            default -> 0.03f;
        };
    }

    private static int dur(Random r, float f) {
        return Math.max(600, (int) (f * Seasons.length() * (0.6f + r.nextFloat() * 0.8f)));
    }

    private static void weather(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        if (w == null || !w.getDimension().hasSkyLight() || !(w.getLevelProperties() instanceof ServerWorldProperties pr)) return;
        boolean rain = w.isRaining(), thunder = w.isThundering();
        int timer = rain ? pr.getRainTime() : pr.getClearWeatherTime() > 0 ? pr.getClearWeatherTime() : pr.getRainTime();
        if (!w.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
            wasRaining = rain;
            lastTimer = timer;
            return;
        }
        Random r = w.getRandom();
        boolean flip = rain != wasRaining;
        boolean jump = timer > lastTimer + 5;
        wasRaining = rain;
        lastTimer = timer;
        if (hold > 0) hold -= 20;
        // силы бога задают ровно 6000 (дождь) или 12000 (ясно): не спорим с ними
        if ((flip || jump) && (rain ? timer > 5800 && timer <= 6000 : timer > 11800 && timer <= 12000)) {
            hold = 6000;
            return;
        }
        if (hold > 0) return;
        float[] f = frac(Seasons.index());
        int max = Math.round(f[rain ? 1 : 0] * Seasons.length() * 1.5f) + 600;
        if (!flip && !jump && timer <= max) return;
        int d = dur(r, f[rain ? 1 : 0]);
        boolean th = rain && (thunder && r.nextFloat() < 0.5f || r.nextFloat() < thunderChance(Seasons.index()));
        w.setWeather(0, d, rain, th);
        lastTimer = d;
    }

    // ---------------- смена сезона и праздник урожая ----------------
    static void onSeason(MinecraftServer s, int se) {
        festival = false;
        if (se == Seasons.WINTER) {
            Chronicle.log(null, Chronicle.Type.SCIENCE, false, null, "\u2744 Выпал первый снег: пришла зима");
            if (s == null) return;
            for (Kingdoms.K k : Kingdoms.K.values()) {
                if (!k.alive || k.world == null || k.capital == null || !k.world.shouldTickEntity(k.capital)) continue;
                Civ.City c = Civ.city(k);
                int need = (k.members.size() + 4) / (c.count(Civ.Kind.GRANARY) > 0 ? 8 : 5) * 6;
                if (c.food < Math.max(8, need)) {
                    Chronicle.say(s, k, Chronicle.Type.ECONOMY, false, null, "— зима пришла, а амбары почти пусты: людям грозит голод");
                }
            }
        }
    }

    /** Осенью, на середине сезона, города празднуют урожай: частицы, звук и прибавка еды. */
    private static void festival(MinecraftServer s) {
        if (festival || Seasons.index() != Seasons.AUTUMN || Seasons.progress() < 0.5f) return;
        festival = true;
        for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive || k.world == null || k.capital == null || !k.world.shouldTickEntity(k.capital)) continue;
            Civ.City c = Civ.city(k);
            int farms = c.count(Civ.Kind.FARM);
            if (farms == 0) continue;
            int bonus = Math.min(30, 4 + farms * 3);
            c.food += bonus;
            BlockPos p = k.capital;
            Fx.particles(k.world, ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 3, p.getZ() + 0.5, 40, 6, 2, 6, 0.05);
            Fx.particles(k.world, ParticleTypes.FIREWORK, p.getX() + 0.5, p.getY() + 12, p.getZ() + 0.5, 60, 4, 3, 4, 0.12);
            Fx.sound(k.world, p.getX() + 0.5, p.getY() + 3, p.getZ() + 0.5, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1f);
            Chronicle.say(s, k, Chronicle.Type.ECONOMY, false, null, "— праздник урожая: закрома пополнились на " + bonus + " еды");
        }
    }
}
