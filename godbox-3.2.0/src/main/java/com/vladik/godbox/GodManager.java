package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GodManager {
    private static final Map<UUID, GameMode> PREVIOUS = new HashMap<>();
    /** Тег игрока хранится в его данных рядом с режимом игры, поэтому переживает аварийное завершение. */
    private static final String TAG = "godbox_prev_";
    private static int timer;

    private GodManager() {}

    /** Вызывается из Systems.registerDefaults (до загрузки мира). */
    static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> restoreLeftover(handler.player));
    }

    public static boolean isGod(ServerPlayerEntity p) {
        return PREVIOUS.containsKey(p.getUuid());
    }

    public static boolean isGodEntity(Entity e) {
        return e instanceof ServerPlayerEntity sp && isGod(sp);
    }

    private static boolean permitted(ServerPlayerEntity p) {
        return p.hasPermissionLevel(2) || p.getServer().isHost(p.getGameProfile());
    }

    private static void clearTags(ServerPlayerEntity p) {
        for (String t : new ArrayList<>(p.getCommandTags())) if (t.startsWith(TAG)) p.removeCommandTag(t);
    }

    public static void setGod(ServerPlayerEntity p, boolean on) {
        ServerWorld w = p.getServerWorld();
        if (on && !isGod(p)) {
            if (!permitted(p)) {
                p.sendMessage(Text.literal("Нужны права оператора, чтобы стать богом").formatted(Formatting.RED), false);
                return;
            }
            GameMode prev = p.interactionManager.getGameMode();
            PREVIOUS.put(p.getUuid(), prev);
            clearTags(p);
            p.addCommandTag(TAG + prev.getName());
            p.changeGameMode(GameMode.SPECTATOR);
            int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
            p.networkHandler.requestTeleport(p.getX(), top + 45, p.getZ(), p.getYaw(), 90f);
            ServerPlayNetworking.send(p, new Payloads.State(true));
            p.sendMessage(Text.literal("Ты бог. Esc или G — вернуться.").formatted(Formatting.GOLD), true);
        } else if (!on && isGod(p)) {
            GameMode prev = PREVIOUS.remove(p.getUuid());
            clearTags(p);
            p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
            int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
            p.networkHandler.requestTeleport(p.getX(), top + 1, p.getZ(), p.getYaw(), 0f);
            p.fallDistance = 0;
            ServerPlayNetworking.send(p, new Payloads.State(false));
        }
    }

    public static void onDisconnect(ServerPlayerEntity p) {
        if (!isGod(p)) return;
        GameMode prev = PREVIOUS.remove(p.getUuid());
        clearTags(p);
        p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
        int top = p.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
        p.refreshPositionAfterTeleport(p.getX(), top + 1, p.getZ());
        p.fallDistance = 0;
    }

    /** Игрок зашёл с тегом бога, но не в списке богов: прошлый сеанс оборвался — вернуть режим. */
    private static void restoreLeftover(ServerPlayerEntity p) {
        if (isGod(p)) return;
        GameMode prev = null;
        boolean found = false;
        for (String t : p.getCommandTags()) {
            if (!t.startsWith(TAG)) continue;
            found = true;
            GameMode m = GameMode.byName(t.substring(TAG.length()), null);
            if (m != null && m != GameMode.SPECTATOR) prev = m;
        }
        if (!found) return;
        clearTags(p);
        p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
        int top = p.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
        p.networkHandler.requestTeleport(p.getX(), Math.max(p.getY(), top + 1), p.getZ(), p.getYaw(), 0f);
        p.fallDistance = 0;
        GodBox.LOG.info("GodBox: {} вошёл с незавершённым режимом бога — режим игры восстановлен", p.getName().getString());
    }

    /** Раз в секунду: бог, лишившийся прав оператора, перестаёт быть богом. */
    public static void tick(MinecraftServer s) {
        if (PREVIOUS.isEmpty() || ++timer < 20) return;
        timer = 0;
        for (UUID id : new ArrayList<>(PREVIOUS.keySet())) {
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p == null || permitted(p)) continue;
            setGod(p, false);
            p.sendMessage(Text.literal("Права оператора сняты — режим бога отключён").formatted(Formatting.RED), false);
        }
    }

    public static void clear() {
        PREVIOUS.clear();
        timer = 0;
    }
}
