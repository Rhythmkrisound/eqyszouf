package com.vladik.godbox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Дороги между городами и союзными столицами: тропа → гравий → булыжник → каменные кирпичи, мосты через воду. */
public final class Roads {
    public static boolean on = true;
    static final Set<String> DONE = new HashSet<>();
    private static int t;
    private static Set<Block> fam;

    private Roads() {}

    static String key(BlockPos a, BlockPos b) {
        String sa = a.getX() + "," + a.getZ(), sb = b.getX() + "," + b.getZ();
        return sa.compareTo(sb) < 0 ? sa + ">" + sb : sb + ">" + sa;
    }

    static BlockState mat(int era, Random r) {
        if (era <= 1) return Blocks.DIRT_PATH.getDefaultState();
        if (era <= 3) return (r.nextInt(4) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL).getDefaultState();
        if (era <= 5) return (r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState();
        return (r.nextInt(6) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState();
    }

    static boolean natural(BlockState s) {
        return s.isIn(BlockTags.DIRT) || s.isOf(Blocks.SAND) || s.isOf(Blocks.RED_SAND) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.DIRT_PATH)
                || s.isOf(Blocks.COBBLESTONE) || s.isOf(Blocks.STONE) || s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.COARSE_DIRT);
    }

    /** Естественный грунт, по которому можно мостить: не постройка, не грядка, не руда. */
    static boolean terrain(BlockState s) {
        return natural(s) || s.isIn(BlockTags.SAND) || s.isIn(BlockTags.TERRACOTTA) || s.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || s.isOf(Blocks.SANDSTONE) || s.isOf(Blocks.RED_SANDSTONE) || s.isOf(Blocks.CLAY);
    }

    /** Любой материал полотна или настила моста любой эпохи (для смены облика). */
    static boolean fam(BlockState s) {
        if (fam == null) {
            Set<Block> f = new HashSet<>();
            Random r = Random.create(7);
            for (int era = 0; era <= 11; era++)
                for (int i = 0; i < 54; i++)
                    for (int n = 0; n < 4; n++) {
                        f.add(Highways.road(era, false, i, r).getBlock());
                        f.add(Highways.road(era, true, i, r).getBlock());
                        f.add(mat(era, r).getBlock());
                        f.add(Highways.bridge(era, false).getBlock());
                        f.add(Highways.bridge(era, true).getBlock());
                    }
            fam = f;
        }
        return fam.contains(s.getBlock());
    }

    static boolean road(BlockState s) {
        return s.isOf(Blocks.OAK_PLANKS) || fam(s) || Highways.paved(s);
    }

    static boolean inBuilding(List<Civ.Building> bs, int x, int z) {
        for (Civ.Building b : bs) if (x >= b.x0 - 1 && x <= b.x1 + 1 && z >= b.z0 - 1 && z <= b.z1 + 1) return true;
        return false;
    }

    /** Узкая дорога строится общим движком Highways: тот же обход препятствий, профиль, мосты и смена облика по эпохам. */
    static boolean build(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b) {
        return Highways.start(w, k, a, b, true);
    }

    static boolean tryRoad(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b, int max) {
        if (a.equals(b) || a.getSquaredDistance(b) > (double) max * max || a.getSquaredDistance(b) < 20 * 20) return false;
        String key = key(a, b);
        if (DONE.contains(key) || Highways.DONE.contains(key) || Highways.active(key)) return false;
        Civ.City c = Civ.city(k);
        if (c.stone < 10) return false;
        if (!build(w, k, a, b)) return false;
        c.stone -= 10;
        DONE.add(key);
        Chronicle.say(w.getServer(), k, "прокладывает дорогу");
        return true;
    }

    public static void tick(MinecraftServer s) {
        ++t;
        if (t % 40 == 0) speed();
        if (!on || t % 1200 != 0) return;
        for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive || k.capital == null || !(k.world instanceof ServerWorld w) || !Tech.has(k, Tech.T.WHEEL)) continue;
            if (t % 2400 != 0 && !Tech.has(k, Tech.T.ENGINEERING)) continue; // инженеры строят вдвое чаще
            boolean built = false;
            for (BlockPos tp : Conquest.towns(k)) if (tryRoad(w, k, k.capital, tp, Tech.roadReach(k, 110))) { built = true; break; }
            if (built) continue;
            for (Kingdoms.K o : Kingdoms.K.values()) {
                if (o == k || !o.alive || o.capital == null || o.world != k.world || Diplomacy.rel(k, o) != Diplomacy.ALLY) continue;
                if (tryRoad(w, k, k.capital, o.capital, Tech.roadReach(k, 110))) break;
            }
        }
    }

    static void speed() {
        for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive || !(k.world instanceof ServerWorld w)) continue;
            for (MobEntity m : k.members) {
                if (!m.isAlive() || !m.isOnGround()) continue;
                if (road(w.getBlockState(m.getBlockPos().down()))) m.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false));
            }
        }
    }

    public static void power(net.minecraft.server.network.ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
        Kingdoms.K k = Tech.nearest(w, pos);
        if (k == null || k.capital == null) return;
        BlockPos target = Fx.surface(w, pos.getX(), pos.getZ());
        DONE.remove(key(k.capital, target));
        Highways.forget(key(k.capital, target));
        Civ.city(k).stone += 10;
        if (!tryRoad(w, k, k.capital, target, 400)) {
            god.sendMessage(net.minecraft.text.Text.literal("Слишком близко к столице или слишком далеко"), true);
        }
    }

    public static void clear() { DONE.clear(); t = 0; }
}
