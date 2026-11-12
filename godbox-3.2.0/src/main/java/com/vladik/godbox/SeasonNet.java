package com.vladik.godbox;

import com.vladik.godbox.net.SeasonPayloads;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** v3.2: рассылка сезона клиентам: при входе, при смене и редко для плавного перехода цвета. */
public final class SeasonNet {
    private static int key = -1, n;

    private SeasonNet() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(SeasonPayloads.Sync.ID, SeasonPayloads.Sync.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, build()));
    }

    private static SeasonPayloads.Sync build() {
        boolean on = Seasons.on;
        return new SeasonPayloads.Sync(on, on && Config.b("seasons.tint", true), Seasons.index(), Math.round(Seasons.progress() * 1000f),
            Seasons.year(), Seasons.length(), Seasons.effects());
    }

    private static void send(ServerPlayerEntity p, SeasonPayloads.Sync m) {
        if (ServerPlayNetworking.canSend(p, SeasonPayloads.Sync.ID)) ServerPlayNetworking.send(p, m);
    }

    static void tick(MinecraftServer s) {
        int k = (Seasons.on ? 1 : 0) | (Config.b("seasons.tint", true) ? 2 : 0) | Seasons.index() << 2;
        if (k != key) {
            key = k;
            n = 0;
            sendAll(s);
        } else if (++n >= 100) {
            n = 0;
            sendAll(s);
        }
    }

    static void sendAll(MinecraftServer s) {
        if (s.getPlayerManager().getPlayerList().isEmpty()) return;
        SeasonPayloads.Sync m = build();
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) send(p, m);
    }
}
