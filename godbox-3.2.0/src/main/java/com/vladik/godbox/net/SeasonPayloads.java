package com.vladik.godbox.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** v3.2: состояние сезона для клиента (окраска листвы, панель бога). Идёт всем игрокам, у кого есть мод. */
public final class SeasonPayloads {
    private SeasonPayloads() {}

    /** on — закон включён; tint — красить траву и листву; progress — доля сезона в промилле; effects — краткая сводка для панели. */
    public record Sync(boolean on, boolean tint, int season, int progress, int year, int length, String effects) implements CustomPayload {
        public static final CustomPayload.Id<Sync> ID = new CustomPayload.Id<>(Identifier.of("godbox", "season"));
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC = PacketCodec.of((v, buf) -> {
            buf.writeBoolean(v.on);
            buf.writeBoolean(v.tint);
            buf.writeVarInt(v.season);
            buf.writeVarInt(v.progress);
            buf.writeVarInt(v.year);
            buf.writeVarInt(v.length);
            PacketCodecs.string(120).encode(buf, v.effects);
        }, buf -> new Sync(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                PacketCodecs.string(120).decode(buf)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
