package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** Единый композитор визуала взрывов: огненный шар, ударное кольцо, столб дыма, искры, обломки, подпалины. */
public final class Blast {
    private static final Map<ServerWorld, State> STATES = new WeakHashMap<>();

    private static final class State {
        long tick = -1;
        int used;
        int fullFx;
        int scorches;
        final double[] xs = new double[8], zs = new double[8];
    }

    private Blast() {}

    private static State state(ServerWorld w) {
        State s = STATES.computeIfAbsent(w, k -> new State());
        long t = w.getTime();
        if (s.tick != t) {
            s.tick = t;
            s.used = 0;
            s.fullFx = 0;
            s.scorches = 0;
        }
        return s;
    }

    /** Сколько частиц из желаемых можно отправить в этом тике; остаток бюджета списывается. */
    private static int take(ServerWorld w, int want) {
        State s = state(w);
        int left = Config.i("fx.blast.budget", 500) - s.used;
        int n = Math.max(0, Math.min(want, left));
        s.used += n;
        return n;
    }

    /** Отправка частицы игрокам поблизости; count = 0, чтобы «скорость» дошла до клиента как есть. */
    private static void send(ServerWorld w, ParticleEffect e, boolean far, double x, double y, double z, double vx, double vy, double vz) {
        double range = far ? 160 : 48;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.squaredDistanceTo(x, y, z) <= range * range && ServerPlayNetworking.canSend(p, Payloads.State.ID)) {
                w.spawnParticles(p, e, far, x, y, z, 0, vx, vy, vz, 1.0);
            }
        }
    }

    public static void fx(ServerWorld w, double x, double y, double z, float power) {
        if (!Config.b("fx.blast.enabled", true)) return;
        if (!(power >= 0.5f) || Float.isInfinite(power)) return;
        if (w.getPlayers().isEmpty()) return;
        if (!w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) return;

        double scale = MathHelper.clamp(Config.d("fx.blast.scale", 1.0), 0.0, 3.0);
        if (scale <= 0) return;
        State st = state(w);
        // много взрывов в одной точке за тик (кассета) — полный эффект только у первых
        int near = 0;
        for (int i = 0; i < Math.min(st.fullFx, st.xs.length); i++) {
            double dx = st.xs[i] - x, dz = st.zs[i] - z;
            if (dx * dx + dz * dz < 36) near++;
        }
        if (near >= 3 || st.fullFx >= Config.i("fx.blast.maxFull", 24)) return;
        if (st.fullFx < st.xs.length) {
            st.xs[st.fullFx] = x;
            st.zs[st.fullFx] = z;
        }
        st.fullFx++;

        float p = Math.min(power, 60f);
        boolean small = p < 2.5f;
        boolean large = p > 8f;
        boolean far = p >= 6f;
        float half = (float) Math.min(26.0, (0.7 + 0.55 * p) * scale);
        int life = (int) Math.min(30, 10 + p * 0.5f);

        // огненный шар
        send(w, GodParticles.FIREBALL, far, x, y, z, half * (small ? 0.8 : 1.0), life, 0);
        if (!small) {
            int blobs = take(w, Math.min(6, (int) (p / 1.5f)));
            var rnd = ThreadLocalRandom.current();
            for (int i = 0; i < blobs; i++) {
                double r = half * 0.55;
                send(w, GodParticles.FIREBALL, far, x + (rnd.nextDouble() - 0.5) * r * 2, y + rnd.nextDouble() * r * (large ? 2.0 : 1.0),
                        z + (rnd.nextDouble() - 0.5) * r * 2, half * (0.45 + rnd.nextDouble() * 0.25), life - 2 + rnd.nextInt(6), 0);
            }
        }

        // ударное кольцо по земле
        if (!small) {
            double gy = groundY(w, x, y, z);
            if (gy > Double.NEGATIVE_INFINITY) {
                float rad = (float) Math.min(40.0, (2.5 + p * 1.2) * scale);
                int rl = (int) Math.min(30, 10 + rad * 0.4);
                send(w, GodParticles.SHOCKWAVE, far, x, gy + 0.06, z, rad, rl, 0);
                if (large) {
                    double fx = x, fz = z, fy = gy + 0.08;
                    Tasks.delay(4, () -> send(w, GodParticles.SHOCKWAVE, true, fx, fy, fz, rad * 1.5, rl + 6, 0));
                }
            }
        }

        // искры и обломки
        var rnd = ThreadLocalRandom.current();
        int embers = take(w, (int) Math.min(40, (small ? 3 : 6) + p * 2.2 * scale));
        for (int i = 0; i < embers; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, sp = (0.12 + rnd.nextDouble() * 0.3) * (0.7 + p * 0.06);
            send(w, GodParticles.EMBER, far, x, y + 0.3, z, Math.cos(a) * sp, 0.25 + rnd.nextDouble() * 0.45, Math.sin(a) * sp);
        }
        if (!small) {
            int debris = take(w, (int) Math.min(26, 4 + p * 2 * scale));
            for (int i = 0; i < debris; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, sp = (0.1 + rnd.nextDouble() * 0.35) * (0.8 + p * 0.05);
                send(w, GodParticles.DEBRIS, far, x, y + 0.2, z, Math.cos(a) * sp, 0.2 + rnd.nextDouble() * 0.5, Math.sin(a) * sp);
            }
        }

        // дым: короткий «пых» у мелких, столб у средних и больших
        if (small) {
            int puffs = take(w, 2);
            for (int i = 0; i < puffs; i++) {
                send(w, GodParticles.PLUME, false, x + (rnd.nextDouble() - 0.5) * 0.6, y + 0.3, z + (rnd.nextDouble() - 0.5) * 0.6, 0.7, 0.03, 30 + rnd.nextInt(15));
            }
        } else {
            plume(w, x, y, z, p, far, scale);
        }

        if (large) rain(w, x, y, z, p, scale);
        sounds(w, x, y, z, p);
        if (p >= 3f && Config.b("fx.blast.scorch", true) && st.scorches++ < Config.i("fx.blast.maxScorch", 12)) scorch(w, x, y, z, p);
    }

    private static void plume(ServerWorld w, double x, double y, double z, float p, boolean far, double scale) {
        boolean large = p > 8f;
        int ticks = large ? (int) Math.min(80, 40 + p) : 16;
        int perTick = large ? 2 : 1;
        float base = (float) ((0.9 + p * 0.1) * scale);
        double rad = Math.min(8, 0.4 + p * 0.12);
        Tasks.add(new Tasks.Task() {
            int t;

            @Override
            public boolean tick() {
                if (!w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) return true;
                var r = ThreadLocalRandom.current();
                int n = take(w, perTick);
                for (int i = 0; i < n; i++) {
                    boolean cap = large && t > ticks * 0.55;
                    double spread = cap ? rad * 2.8 : rad * (0.5 + 0.02 * t);
                    double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * spread;
                    double py = y + 0.5 + t * (large ? 0.4 : 0.2) + r.nextDouble();
                    send(w, GodParticles.PLUME, far, x + Math.cos(a) * d, py, z + Math.sin(a) * d,
                            base * (0.7 + r.nextDouble() * 0.6), 0.03 + r.nextDouble() * 0.03, (large ? 90 : 45) + r.nextInt(50));
                }
                return ++t >= ticks;
            }
        });
    }

    /** Тлеющий дождь из искр над крупным взрывом. */
    private static void rain(ServerWorld w, double x, double y, double z, float p, double scale) {
        double rad = Math.min(24, 4 + p * 0.5);
        Tasks.add(new Tasks.Task() {
            int t;

            @Override
            public boolean tick() {
                if (!w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) return true;
                var r = ThreadLocalRandom.current();
                int n = take(w, (int) (3 * scale));
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * rad;
                    send(w, GodParticles.EMBER, true, x + Math.cos(a) * d, y + 8 + r.nextDouble() * 10, z + Math.sin(a) * d,
                            (r.nextDouble() - 0.5) * 0.06, -0.05, (r.nextDouble() - 0.5) * 0.06);
                }
                return ++t >= 50;
            }
        });
    }

    private static void sounds(ServerWorld w, double x, double y, double z, float p) {
        if (!Config.b("fx.blast.sound", true)) return;
        if (p >= 5f) {
            float v = Math.min(4f, 1.5f + p * 0.1f);
            Tasks.delay(5, () -> w.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, v, 0.5f));
        }
        if (p >= 10f) {
            Tasks.delay(14, () -> w.playSound(null, x, y, z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.BLOCKS, 3f, 0.55f));
        }
    }

    /** Земля под центром взрыва в пределах 8 блоков; MIN_VALUE, если взрыв в воздухе. */
    private static double groundY(ServerWorld w, double x, double y, double z) {
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        int by = MathHelper.floor(y);
        if (!w.isChunkLoaded(bx >> 4, bz >> 4)) return Double.NEGATIVE_INFINITY;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dy = 0; dy <= 8; dy++) {
            m.set(bx, by - dy, bz);
            if (w.isOutOfHeightLimit(m)) break;
            BlockState s = w.getBlockState(m);
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.getCollisionShape(w, m).isEmpty()) {
                return m.getY() + 1;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    private static BlockState charred(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.PODZOL || b == Blocks.MYCELIUM) return Blocks.COARSE_DIRT.getDefaultState();
        if (b == Blocks.SAND || b == Blocks.RED_SAND) return Blocks.GRAVEL.getDefaultState();
        if (b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.ANDESITE || b == Blocks.DIORITE || b == Blocks.GRANITE) return Blocks.BLACKSTONE.getDefaultState();
        if (b == Blocks.DEEPSLATE) return Blocks.COBBLED_DEEPSLATE.getDefaultState();
        return null;
    }

    /** Редкие подпалины на верхних гранях вокруг эпицентра; блоки меняются порциями по тикам через обычный setBlockState (Undo пишет их сам). */
    private static void scorch(ServerWorld w, double x, double y, double z, float p) {
        int rad = (int) Math.min(12, 2 + p * 1.1f);
        int total = (int) Math.min(90, 6 + p * 5);
        Tasks.add(new Tasks.Task() {
            int done;

            @Override
            public boolean tick() {
                var r = ThreadLocalRandom.current();
                BlockPos.Mutable m = new BlockPos.Mutable();
                for (int i = 0; i < 12 && done < total; i++, done++) {
                    double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * rad;
                    int bx = MathHelper.floor(x + Math.cos(a) * d), bz = MathHelper.floor(z + Math.sin(a) * d);
                    if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                    for (int by = MathHelper.floor(y) + 3; by >= MathHelper.floor(y) - rad; by--) {
                        m.set(bx, by, bz);
                        if (w.isOutOfHeightLimit(m)) break;
                        BlockState s = w.getBlockState(m);
                        if (s.isAir()) continue;
                        BlockState n = charred(s);
                        if (n != null && w.getBlockState(m.up()).isAir() && !w.getBlockState(m).hasBlockEntity()) {
                            w.setBlockState(m, n, Block.NOTIFY_LISTENERS);
                        }
                        break;
                    }
                }
                return done >= total;
            }
        });
    }
}
