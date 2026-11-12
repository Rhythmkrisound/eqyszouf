package com.vladik.godbox;

import com.vladik.godbox.mixin.DisplayEntityAccessor;
import com.vladik.godbox.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Метеориты: модель в полёте, останки в кратере, метеоритные дожди (божественные и природные). */
public final class Meteors {
    private Meteors() {}

    /** Размеры: 0 — мелкий, 1 — средний, 2 — крупный (как прежний одиночный метеорит). */
    public static final int SMALL = 0, MEDIUM = 1, BIG = 2;

    private static final List<Body> LIVE = new ArrayList<>();

    /** Сколько метеоритов сейчас показано моделью (для ограничения одновременных). */
    public static int live() {
        for (Iterator<Body> it = LIVE.iterator(); it.hasNext(); ) {
            Body b = it.next();
            if (b.dead()) it.remove();
        }
        return LIVE.size();
    }

    // ---------------------------------------------------------------- модель в полёте

    /** Падающая глыба: две детали (порода и жилы) на безымянных display-сущностях, которые не сохраняются. */
    public static final class Body {
        private static final int MAX_AGE = 400;
        private final ServerWorld w;
        private final DisplayEntity.ItemDisplayEntity[] ents;
        private final float scale;
        private final Vector3f axis;
        private final float spin;
        private final Quaternionf start;
        private int age;
        private boolean gone;

        private Body(ServerWorld w, DisplayEntity.ItemDisplayEntity[] ents, float scale, Random r) {
            this.w = w;
            this.ents = ents;
            this.scale = scale;
            this.axis = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
            if (axis.lengthSquared() < 0.01f) axis.set(0.3f, 1f, 0.2f);
            axis.normalize();
            this.spin = (0.12f + r.nextFloat() * 0.14f) * (r.nextBoolean() ? 1 : -1);
            this.start = new Quaternionf().rotationXYZ(r.nextFloat() * 6.28f, r.nextFloat() * 6.28f, r.nextFloat() * 6.28f);
        }

        /** null — модели нет или models.custom=false: вызывающий оставляет старый вид из частиц. */
        static Body create(ServerWorld w, double x, double y, double z, int size, Random r) {
            if (!Models.custom()) return null;
            String model = r.nextBoolean() ? "meteor" : "meteor_b";
            ItemStack[] stacks = {Models.part(model, "rock"), Models.part(model, "glow")};
            if (stacks[0] == null || stacks[1] == null) return null;
            float scale = size == BIG ? 1.5f : size == MEDIUM ? 0.95f : 0.45f + r.nextFloat() * 0.2f;
            DisplayEntity.ItemDisplayEntity[] ents = new DisplayEntity.ItemDisplayEntity[2];
            Body b = new Body(w, ents, scale, r);
            for (int i = 0; i < 2; i++) {
                final Body self = b;
                DisplayEntity.ItemDisplayEntity e = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, w) {
                    @Override
                    public boolean shouldSave() {
                        return false;
                    }

                    private int life;

                    @Override
                    public void tick() {
                        super.tick();
                        // страховка: осиротевшая деталь (задача сброшена) исчезает сама
                        if (!isRemoved() && (++life > MAX_AGE || self.age > MAX_AGE)) discard();
                    }
                };
                e.setItemStack(stacks[i]);
                e.setTeleportDuration(2);
                e.setInterpolationDuration(2);
                e.setViewRange(4f);
                e.setDisplayWidth(8f);
                e.setDisplayHeight(8f);
                if (i == 1) e.setBrightness(new Brightness(15, 15));
                e.refreshPositionAndAngles(x, y, z, 0f, 0f);
                ents[i] = e;
            }
            b.pose(true);
            for (int i = 0; i < 2; i++) {
                if (!w.spawnEntity(ents[i])) {
                    b.discard();
                    return null;
                }
            }
            LIVE.add(b);
            return b;
        }

        boolean dead() {
            if (gone) return true;
            return ents[0].isRemoved() || ents[1].isRemoved();
        }

        void move(double x, double y, double z) {
            age++;
            for (DisplayEntity.ItemDisplayEntity e : ents) e.refreshPositionAndAngles(x, y, z, 0f, 0f);
            if (age % 2 == 0) pose(false);
        }

        private void pose(boolean force) {
            Quaternionf q = new Quaternionf().rotationAxis(spin * age, axis.x, axis.y, axis.z).mul(start);
            for (DisplayEntity.ItemDisplayEntity e : ents) {
                e.setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(q), new Vector3f(scale, scale, scale), new Quaternionf()));
                if (!force) e.getDataTracker().set(DisplayEntityAccessor.godbox$startInterpolation(), 0, true);
            }
        }

        void discard() {
            gone = true;
            for (DisplayEntity.ItemDisplayEntity e : ents) if (e != null) e.discard();
            LIVE.remove(this);
        }
    }

    // ---------------------------------------------------------------- эффекты

    /** Частицы только игрокам поблизости (в отличие от Fx.particles, который шлёт всем в мире). */
    public static void spray(ServerWorld w, ParticleEffect e, double x, double y, double z, int count, double d, double speed) {
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.squaredDistanceTo(x, y, z) <= 128 * 128) w.spawnParticles(p, e, true, x, y, z, count, d, d, d, speed);
        }
    }

    /** Искры GodParticles.EMBER: только клиентам с модом (скорость уходит как есть при count=0). */
    public static void ember(ServerWorld w, double x, double y, double z, double vx, double vy, double vz) {
        if (GodParticles.EMBER == null) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.squaredDistanceTo(x, y, z) <= 128 * 128 && ServerPlayNetworking.canSend(p, Payloads.State.ID)) {
                w.spawnParticles(p, GodParticles.EMBER, true, x, y, z, 0, vx, vy, vz, 1.0);
            }
        }
    }

    // ---------------------------------------------------------------- останки

    private static boolean loaded(ServerWorld w, int x, int z) {
        return w.isChunkLoaded(x >> 4, z >> 4);
    }

    /** Кусок метеорита, остывающий со временем: магма превращается в чёрный камень. */
    public static void remnant(ServerWorld w, BlockPos ground, int size, boolean water) {
        if (!Config.b("meteor.remnant", true) || size == SMALL && w.getRandom().nextInt(4) != 0) return;
        Random r = w.getRandom();
        int oreChance = MathHelper.clamp(Config.i("meteor.ore", 35), 0, 100);
        List<BlockPos> cells = new ArrayList<>();
        cells.add(BlockPos.ORIGIN);
        if (size >= MEDIUM) {
            for (Direction d : Direction.Type.HORIZONTAL) if (r.nextFloat() < (size == BIG ? 0.85f : 0.5f)) cells.add(BlockPos.ORIGIN.offset(d));
        }
        if (size == BIG) {
            if (r.nextBoolean()) cells.add(new BlockPos(1, 0, 1));
            if (r.nextBoolean()) cells.add(new BlockPos(-1, 0, -1));
        }
        boolean ore = size >= MEDIUM && r.nextInt(100) < (size == BIG ? oreChance : oreChance / 3);
        int cool = Math.max(100, Config.i("meteor.cool", 600));
        boolean first = true;
        for (BlockPos c : cells) {
            int x = ground.getX() + c.getX(), z = ground.getZ() + c.getZ();
            if (!loaded(w, x, z) || inBuilding(w, x, z)) continue;
            BlockPos top = w.getTopPosition(Heightmap.Type.OCEAN_FLOOR, new BlockPos(x, 0, z));
            int h = first && size == BIG ? 2 : 1;
            for (int i = 0; i < h; i++) {
                BlockPos p = top.up(i);
                BlockState cur = w.getBlockState(p);
                if (!(cur.isAir() || cur.isReplaceable() || cur.getFluidState().isStill() && water)) break;
                if (!w.getBlockState(p.down()).isSideSolidFullSquare(w, p.down(), Direction.UP)) break;
                BlockState put;
                if (ore && first && i == h - 1) {
                    int k = r.nextInt(100);
                    put = k < 12 ? Blocks.ANCIENT_DEBRIS.getDefaultState() : k < 45 ? Blocks.DEEPSLATE_GOLD_ORE.getDefaultState()
                            : Blocks.DEEPSLATE_IRON_ORE.getDefaultState();
                } else {
                    int k = r.nextInt(100);
                    put = k < 45 ? Blocks.BLACKSTONE.getDefaultState() : k < 70 ? Blocks.BASALT.getDefaultState()
                            : water || k < 95 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.GILDED_BLACKSTONE.getDefaultState();
                    if (!water && i == 0 && k >= 70 && r.nextBoolean()) put = Blocks.MAGMA_BLOCK.getDefaultState();
                }
                w.setBlockState(p, put, 3);
                if (put.isOf(Blocks.MAGMA_BLOCK)) coolLater(w, p, cool + r.nextInt(cool));
            }
            first = false;
        }
    }

    private static void coolLater(ServerWorld w, BlockPos p, int ticks) {
        Tasks.delay(ticks, () -> {
            if (loaded(w, p.getX(), p.getZ()) && w.getBlockState(p).isOf(Blocks.MAGMA_BLOCK)) {
                w.setBlockState(p, Blocks.BLACKSTONE.getDefaultState(), 3);
                spray(w, net.minecraft.particle.ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 4, 0.3, 0.02);
            }
        });
    }

    // ---------------------------------------------------------------- дожди

    private record Plan(int delay, BlockPos target, int size, boolean gentle) {}

    /** Поток метеоритов: общее направление, разнотипные размеры, лимит одновременных и общего числа. */
    private static final class Shower implements Tasks.Task {
        private final ServerWorld w;
        private final List<Plan> plans;
        private final double heading;
        private int t;

        Shower(ServerWorld w, List<Plan> plans, double heading) {
            this.w = w;
            this.plans = plans;
            this.heading = heading;
            plans.sort(Comparator.comparingInt(Plan::delay));
        }

        @Override
        public boolean tick() {
            t++;
            int cap = Math.max(2, Config.i("meteor.max_active", 14));
            while (!plans.isEmpty() && plans.get(0).delay <= t) {
                Plan p = plans.get(0);
                // при перегрузке запуск откладывается, а не теряется; совсем опоздавшие метеориты пропускаются
                if (live() >= cap) {
                    if (t - p.delay > 80) {
                        plans.remove(0);
                        continue;
                    }
                    break;
                }
                plans.remove(0);
                if (loaded(w, p.target.getX(), p.target.getZ())) {
                    Tasks.add(new Disasters.Meteor(w, p.target, p.size, heading + (w.getRandom().nextDouble() - 0.5) * 0.12, p.gentle));
                }
            }
            return plans.isEmpty();
        }
    }

    private static BlockPos ground(ServerWorld w, int x, int z) {
        if (!loaded(w, x, z)) return null;
        return Fx.surface(w, x, z).down();
    }

    /** Божественный метеоритный дождь: в основном мелкие, несколько средних и крупных. */
    public static void rain(ServerWorld w, BlockPos pos, int brush) {
        Random r = w.getRandom();
        int spread = 8 + brush * 2;
        int total = Math.min(Math.max(6, Config.i("meteor.shower.max", 36)), 14 + brush * 2);
        int big = Math.min(4, 2 + brush / 4), medium = Math.max(2, total / 4);
        int span = 100 + brush * 4;
        List<Plan> plans = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            BlockPos g = ground(w, pos.getX() + r.nextInt(spread * 2 + 1) - spread, pos.getZ() + r.nextInt(spread * 2 + 1) - spread);
            if (g == null) continue;
            int size = i < big ? BIG : i < big + medium ? MEDIUM : SMALL;
            plans.add(new Plan(1 + r.nextInt(span), g, size, false));
        }
        if (plans.isEmpty()) return;
        Tasks.add(new Shower(w, plans, r.nextDouble() * Math.PI * 2));
        Chronicle.log(null, Chronicle.Type.DISASTER, false, pos, "Метеоритный дождь: с неба обрушились раскалённые камни");
    }

    /** Не внутри построек и не ближе minD к столицам королевств этого мира. */
    private static boolean outsideTowns(ServerWorld w, int x, int z, int minD) {
        for (Kingdoms.K q : Kingdoms.K.values()) {
            if (!q.alive || q.world != w || q.capital == null) continue;
            long dx = x - q.capital.getX(), dz = z - q.capital.getZ();
            if (dx * dx + dz * dz < (long) minD * minD) return false;
        }
        return !inBuilding(w, x, z);
    }

    private static boolean inBuilding(ServerWorld w, int x, int z) {
        for (Kingdoms.K q : Kingdoms.K.values()) {
            if (!q.alive || q.world != w) continue;
            Civ.City c = Civ.city(q);
            if (c == null) continue;
            for (Civ.Building b : c.buildings) if (b.inside(x, z, 2)) return true;
        }
        return false;
    }

    /**
     * Редкий природный поток: раз в период один бросок на весь мир (в среднем раз в meteor.natural.days игровых суток).
     * Мелкие сгорают в воздухе, иногда один средний падает за пределами города.
     */
    public static void natural(Kingdoms.K[] all, int period) {
        if (!Config.b("meteor.natural", true)) return;
        double days = Math.max(0.5, Config.d("meteor.natural.days", 8.0));
        if (Math.random() >= period / (days * 24000.0)) return;
        List<Kingdoms.K> pool = new ArrayList<>();
        for (Kingdoms.K k : all) {
            if (k.alive && k.world != null && k.capital != null && !k.world.getPlayers().isEmpty()) pool.add(k);
        }
        if (pool.isEmpty() || live() > 0) return;
        Kingdoms.K k = pool.get((int) (Math.random() * pool.size()));
        ServerWorld w = k.world;
        Random r = w.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        int cx = k.capital.getX() + (int) (Math.cos(a) * 60), cz = k.capital.getZ() + (int) (Math.sin(a) * 60);
        List<Plan> plans = new ArrayList<>();
        int n = 6 + r.nextInt(7);
        for (int i = 0; i < n; i++) {
            BlockPos g = ground(w, cx + r.nextInt(61) - 30, cz + r.nextInt(61) - 30);
            if (g != null) plans.add(new Plan(1 + r.nextInt(160), g, SMALL, true));
        }
        if (r.nextInt(100) < 35) {
            Civ.City c = Civ.city(k);
            int town = (c == null ? 44 : Borders.radius(c, false)) + 15;
            for (int tries = 0; tries < 6; tries++) {
                double b = a + (r.nextDouble() - 0.5) * 1.2;
                double d = Math.max(town, 60) + 5 + r.nextDouble() * 25;
                int x = k.capital.getX() + (int) (Math.cos(b) * d), z = k.capital.getZ() + (int) (Math.sin(b) * d);
                if (!outsideTowns(w, x, z, Math.max(town, 60))) continue;
                BlockPos g = ground(w, x, z);
                if (g != null) {
                    plans.add(new Plan(60 + r.nextInt(80), g, MEDIUM, true));
                    break;
                }
            }
        }
        if (plans.isEmpty()) return;
        Tasks.add(new Shower(w, plans, a + Math.PI));
        Chronicle.log(k, Chronicle.Type.DISASTER, false, k.capital, "метеоритный поток прочертил небо над землями королевства");
    }

    public static void clear() {
        for (Body b : new ArrayList<>(LIVE)) b.discard();
        LIVE.clear();
    }
}
