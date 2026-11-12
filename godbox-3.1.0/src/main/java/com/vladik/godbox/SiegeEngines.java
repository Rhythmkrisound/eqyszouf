package com.vladik.godbox;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Осадные орудия на колёсах: катапульта (эпоха механики) и пушка (порох). Орудие — модель на невидимой стойке-носителе:
 * его выкатывают к войску, расчёт (ближайшие солдаты королевства) стреляет из него и катит вслед за осадой,
 * а вражеские солдаты рядом ломают его. Стойки не сохраняются в мир — после перезапуска орудий нет.
 */
public final class SiegeEngines {
    public static final String TAG = "gb_siege";
    /** Результат request: нет своих моделей / орудие занято (выстрела нет) / выстрел сделан. */
    public static final int NONE = 0, WAIT = 1, FIRED = 2;

    public enum Type {
        CATAPULT("catapult", "катапульту", "siege.catapult.", 160, 14, 55, 3.0, 40),
        CANNON("cannon", "пушку", "siege.cannon.", 80, 14, 70, 1.5, 60);

        final String model, ru, key;
        final int reload, min, range, hp;
        final double spread;

        Type(String model, String ru, String key, int reload, int min, int range, double spread, int hp) {
            this.model = model;
            this.ru = ru;
            this.key = key;
            this.reload = reload;
            this.min = min;
            this.range = range;
            this.spread = spread;
            this.hp = hp;
        }

        int reload() { return Math.max(20, Config.i(key + "reload", reload)); }
        int min() { return Config.i(key + "min", min); }
        int range() { return Math.max(min() + 10, Config.i(key + "range", range)); }
        double spread() { return Config.d(key + "spread", spread); }
        float hp() { return (float) Config.d(key + "hp", hp); }
        int count() { return Math.max(1, this == CANNON ? Config.i("guns.cannon.max", 3) : Config.i("siege.catapult.count", 3)); }
        float power() { return (float) (this == CANNON ? Config.d("tech.cannon.power", 3.5) : Config.d("siege.catapult.power", 2.5)); }
    }

    private static final class Engine {
        final ArmorStandEntity stand;
        final Kingdoms.K k;
        final Type type;
        float hp;
        int cool, idle, lonely;
        MobEntity crew;
        boolean go;
        double gx, gz;

        Engine(ArmorStandEntity stand, Kingdoms.K k, Type type) {
            this.stand = stand;
            this.k = k;
            this.type = type;
            this.hp = type.hp();
        }
    }

    private static final List<Engine> ENGINES = new ArrayList<>();
    private static int t;

    private SiegeEngines() {}

    // ---------------------------------------------------------------- заказ выстрела

    /**
     * Солдат m просит выстрел по target. Есть расчёт и цель в дальности — стреляет орудие; расчёт есть, но цель далеко —
     * орудие катят к ней; орудий с расчётом нет — выкатывают новое. NONE — моделей нет, стреляют по-старому.
     */
    static int request(ServerWorld w, Kingdoms.K k, MobEntity m, BlockPos target, Type type) {
        if (!Models.custom() || !ModelParts.has(type.model)) return NONE;
        Engine ready = null, mover = null;
        boolean busy = false, idle = false;
        int own = 0;
        Engine far = null;
        double reach = Config.d("siege.reach", 60);
        for (Engine e : ENGINES) {
            if (e.k != k || e.type != type || e.stand.isRemoved() || e.stand.getWorld() != w) continue;
            own++;
            double dm = e.stand.squaredDistanceTo(m);
            if (far == null || dm > far.stand.squaredDistanceTo(m)) far = e;
            if (e.crew == null || !e.crew.isAlive()) {
                if (dm < reach * reach) idle = true;
                continue;
            }
            double d = flat(e, target);
            if (d >= type.min() && d <= type.range()) {
                if (e.cool > 0) busy = true;
                else if (ready == null || d < flat(ready, target)) ready = e;
            } else if (dm < reach * reach && (mover == null || dm < mover.stand.squaredDistanceTo(m))) mover = e;
        }
        if (ready != null) {
            fire(w, ready, target);
            return FIRED;
        }
        if (busy) return WAIT;
        if (mover != null) {
            push(mover, target);
            return WAIT;
        }
        if (idle) return WAIT;
        if (own >= type.count() && far != null) remove(far);
        boolean first = true;
        for (Engine e : ENGINES) if (e.k == k && !e.stand.isRemoved()) first = false;
        Engine e = deploy(w, k, m, target, type);
        if (e == null) return NONE;
        if (first) Chronicle.say(w.getServer(), k, "выкатывает " + type.ru + " к стенам врага");
        e.cool = 20;
        double d = flat(e, target);
        if (d < type.min() || d > type.range()) push(e, target);
        return WAIT;
    }

    private static double flat(Engine e, BlockPos target) {
        double dx = target.getX() + 0.5 - e.stand.getX(), dz = target.getZ() + 0.5 - e.stand.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static float yawTo(ArmorStandEntity s, double tx, double tz) {
        return (float) (MathHelper.atan2(tz - s.getZ(), tx - s.getX()) * 180.0 / Math.PI) - 90f;
    }

    // ---------------------------------------------------------------- выстрел

    private static void fire(ServerWorld w, Engine e, BlockPos target) {
        ArmorStandEntity s = e.stand;
        double tx = target.getX() + 0.5, tz = target.getZ() + 0.5;
        float yaw = yawTo(s, tx, tz);
        s.refreshPositionAndAngles(s.getX(), s.getY(), s.getZ(), yaw, 0f);
        e.go = false;
        e.cool = e.type.reload();
        e.idle = 0;
        double sp = e.type.spread() * (0.4 + 0.6 * flat(e, target) / e.type.range());
        int gx = MathHelper.floor(tx + w.getRandom().nextGaussian() * sp), gz = MathHelper.floor(tz + w.getRandom().nextGaussian() * sp);
        BlockPos g = w.isChunkLoaded(gx >> 4, gz >> 4) ? Fx.surface(w, gx, gz) : target;
        double rad = yaw * Math.PI / 180.0;
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        Models.trigger(s);
        MobEntity crew = e.crew;
        if (e.type == Type.CANNON) {
            double ox = s.getX() + fx * 1.4, oy = s.getY() + 1.15, oz = s.getZ() + fz * 1.4;
            Tech.cannonball(w, crew, ox, oy, oz, g, Guns.ball(), e.type.power());
            Fx.particles(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, ox, oy, oz, 6, 0.2, 0.2, 0.2, 0.02);
            return;
        }
        double ox = s.getX() + fx * 0.8, oy = s.getY() + 3.0, oz = s.getZ() + fz * 0.8;
        Fx.sound(w, s.getX(), s.getY() + 1, s.getZ(), SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.6f);
        // камень вылетает, когда плечо дошло до верхней точки взмаха
        Tasks.delay(5, () -> {
            if (s.isRemoved() || crew == null || !crew.isAlive() || crew.getWorld() != w) return;
            ItemStack rock = Models.part("guns", "boulder");
            Siege.lob(w, crew, ox, oy, oz, g, rock != null ? rock : new ItemStack(Items.COBBLESTONE), e.type.power());
        });
    }

    // ---------------------------------------------------------------- перекатывание

    /** Орудие нужно подкатить: цель на дальности, чуть ближе края (или чуть дальше от минимума). */
    private static void push(Engine e, BlockPos target) {
        double tx = target.getX() + 0.5, tz = target.getZ() + 0.5;
        double dx = e.stand.getX() - tx, dz = e.stand.getZ() - tz, d = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double want = MathHelper.clamp(d, e.type.min() + 3, e.type.range() - 6);
        e.gx = tx + dx / d * want;
        e.gz = tz + dz / d * want;
        e.go = true;
        e.idle = 0;
        float yaw = yawTo(e.stand, tx, tz);
        e.stand.refreshPositionAndAngles(e.stand.getX(), e.stand.getY(), e.stand.getZ(), yaw, 0f);
    }

    /** Один шаг к цели; расчёт обязателен. Стоп, если впереди вода, лава, обрыв, подъём больше блока или незагруженный чанк. */
    private static void roll(ServerWorld w, Engine e, double step) {
        if (e.crew == null) return;
        ArmorStandEntity s = e.stand;
        double dx = e.gx - s.getX(), dz = e.gz - s.getZ(), d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.3) {
            e.go = false;
            return;
        }
        double nx = s.getX() + dx / d * step, nz = s.getZ() + dz / d * step;
        int y = groundY(w, nx, nz, s.getBlockY());
        if (y == Integer.MIN_VALUE) {
            e.go = false;
            return;
        }
        s.refreshPositionAndAngles(nx, y, nz, s.getYaw(), 0f);
        e.idle = 0;
    }

    private static int groundY(ServerWorld w, double x, double z, int cy) {
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        if (!w.isChunkLoaded(bx >> 4, bz >> 4)) return Integer.MIN_VALUE;
        for (int y = cy + 1; y >= cy - 2; y--) {
            BlockPos feet = new BlockPos(bx, y, bz), below = feet.down();
            BlockState bs = w.getBlockState(below);
            if (!bs.isSideSolidFullSquare(w, below, Direction.UP) || !bs.getFluidState().isEmpty()) continue;
            if (free(w, feet) && free(w, feet.up()) && free(w, feet.up(2))) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean free(ServerWorld w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.getCollisionShape(w, p).isEmpty() && s.getFluidState().isEmpty();
    }

    // ---------------------------------------------------------------- появление и снос

    private static Engine deploy(ServerWorld w, Kingdoms.K k, MobEntity m, BlockPos target, Type type) {
        double dx = target.getX() + 0.5 - m.getX(), dz = target.getZ() + 0.5 - m.getZ(), d = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double side = d - 2 >= type.min() + 1 ? 2 : -2;
        double x = m.getX() + dx / d * side, z = m.getZ() + dz / d * side;
        int y = groundY(w, x, z, m.getBlockY());
        if (y == Integer.MIN_VALUE) {
            x = m.getX();
            z = m.getZ();
            y = groundY(w, x, z, m.getBlockY());
            if (y == Integer.MIN_VALUE) return null;
        }
        // стойка-носитель не сохраняется в мир: орудия — расходная часть осады
        ArmorStandEntity s = new ArmorStandEntity(w, MathHelper.floor(x) + 0.5, y, MathHelper.floor(z) + 0.5) {
            @Override
            public boolean shouldSave() {
                return false;
            }
        };
        s.setInvisible(true);
        s.setMarker(true);
        s.setNoGravity(true);
        s.setInvulnerable(true);
        s.setSilent(true);
        s.addCommandTag(TAG);
        s.refreshPositionAndAngles(s.getX(), s.getY(), s.getZ(), yawTo(s, target.getX() + 0.5, target.getZ() + 0.5), 0f);
        if (!w.spawnEntity(s)) return null;
        if (!Models.attach(s, type.model)) {
            s.discard();
            return null;
        }
        Engine e = new Engine(s, k, type);
        e.crew = m;
        ENGINES.add(e);
        Fx.sound(w, s.getX(), s.getY(), s.getZ(), SoundEvents.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
        return e;
    }

    private static void remove(Engine e) {
        ENGINES.remove(e);
        Models.detach(e.stand);
        e.stand.discard();
    }

    private static void destroy(ServerWorld w, Engine e) {
        ArmorStandEntity s = e.stand;
        Fx.particles(w, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()), s.getX(), s.getY() + 1, s.getZ(), 40, 0.8, 0.6, 0.8, 0.1);
        Fx.particles(w, ParticleTypes.LARGE_SMOKE, s.getX(), s.getY() + 1, s.getZ(), 10, 0.6, 0.4, 0.6, 0.03);
        Fx.sound(w, s.getX(), s.getY(), s.getZ(), SoundEvents.BLOCK_WOOD_BREAK, 1.5f, 0.7f);
        remove(e);
        Chronicle.say(w.getServer(), e.k, "теряет " + e.type.ru + ", разбитую врагом");
    }

    // ---------------------------------------------------------------- тик

    private static boolean atWar(Kingdoms.K k) {
        for (Kingdoms.K o : Kingdoms.K.values()) if (o.alive && o.world == k.world && Diplomacy.hostile(k, o)) return true;
        return false;
    }

    private static MobEntity crewOf(Engine e) {
        double r = Config.d("siege.crew", 6), best = r * r;
        MobEntity c = null;
        for (MobEntity m : e.k.members) {
            if (!m.isAlive() || m instanceof VillagerEntity || m.getWorld() != e.stand.getWorld()) continue;
            double d = m.squaredDistanceTo(e.stand);
            if (d <= best) {
                best = d;
                c = m;
            }
        }
        return c;
    }

    public static void tick(MinecraftServer srv) {
        if (ENGINES.isEmpty()) return;
        t++;
        if (t % 2 == 0) {
            double step = Config.d("siege.speed", 1.2) / 10.0;
            for (Engine e : ENGINES) if (e.go && !e.stand.isRemoved() && e.stand.getWorld() instanceof ServerWorld w) roll(w, e, step);
        }
        if (t % 10 != 0) return;
        int timeout = Config.i("guns.cannon.idle", 1200);
        double dmg = Config.d("siege.damage", 2.0);
        for (Iterator<Engine> it = new ArrayList<>(ENGINES).iterator(); it.hasNext(); ) {
            Engine e = it.next();
            if (!(e.stand.getWorld() instanceof ServerWorld w) || e.stand.isRemoved() || !e.k.alive || e.k.world != w || !atWar(e.k)) {
                remove(e);
                continue;
            }
            e.cool -= 10;
            e.idle += 10;
            e.crew = crewOf(e);
            e.lonely = e.crew == null ? e.lonely + 10 : 0;
            boolean obsolete = e.type == Type.CATAPULT && Tech.has(e.k, Tech.T.GUNPOWDER);
            if (e.idle > timeout && (e.lonely > timeout || obsolete)) {
                remove(e);
                continue;
            }
            int foes = 0;
            for (LivingEntity le : Fx.living(w, e.stand.getPos(), 2.5)) {
                Kingdoms.K o = Kingdoms.of(le);
                if (o != null && o != e.k && !(le instanceof VillagerEntity) && Diplomacy.hostile(e.k, o)) foes++;
            }
            if (foes == 0) continue;
            e.hp -= (float) (foes * dmg);
            if (e.hp <= 0) {
                destroy(w, e);
                continue;
            }
            Fx.particles(w, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()), e.stand.getX(), e.stand.getY() + 1, e.stand.getZ(), 6, 0.5, 0.4, 0.5, 0.05);
            Fx.sound(w, e.stand.getX(), e.stand.getY(), e.stand.getZ(), SoundEvents.BLOCK_WOOD_HIT, 0.8f, 0.8f);
        }
    }

    public static void clear() {
        for (Engine e : ENGINES) {
            Models.detach(e.stand);
            e.stand.discard();
        }
        ENGINES.clear();
        t = 0;
    }
}
