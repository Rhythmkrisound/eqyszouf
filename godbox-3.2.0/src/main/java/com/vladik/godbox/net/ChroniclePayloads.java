package com.vladik.godbox.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** v3.2: пакеты летописи. Все запросы клиента проверяются на сервере (бог, лимиты, обрезка строк). */
public final class ChroniclePayloads {
    public static final int PAGE = 40, TEXT = 300, MAX_PINS = 40, MAX_LINES = 48, LINE = 180;

    private ChroniclePayloads() {}

    /** Запись для клиента; k — id королевства или "" (событие мира). */
    public record Dto(long id, int day, int year, int season, String k, int type, boolean major, String text, boolean hasPos, int x, int z) {
        public static final PacketCodec<RegistryByteBuf, Dto> CODEC = PacketCodec.of((v, buf) -> {
            buf.writeVarLong(v.id);
            buf.writeVarInt(v.day);
            buf.writeVarInt(v.year);
            buf.writeVarInt(v.season + 1);
            PacketCodecs.string(16).encode(buf, v.k);
            buf.writeVarInt(v.type);
            buf.writeBoolean(v.major);
            PacketCodecs.string(TEXT).encode(buf, v.text);
            buf.writeBoolean(v.hasPos);
            if (v.hasPos) {
                buf.writeInt(v.x);
                buf.writeInt(v.z);
            }
        }, buf -> {
            long id = buf.readVarLong();
            int day = buf.readVarInt(), year = buf.readVarInt(), season = buf.readVarInt() - 1;
            String k = PacketCodecs.string(16).decode(buf);
            int type = buf.readVarInt();
            boolean major = buf.readBoolean();
            String text = PacketCodecs.string(TEXT).decode(buf);
            boolean has = buf.readBoolean();
            int x = 0, z = 0;
            if (has) {
                x = buf.readInt();
                z = buf.readInt();
            }
            return new Dto(id, day, year, season, k, type, major, text, has, x, z);
        });
        public static final PacketCodec<RegistryByteBuf, List<Dto>> LIST = PacketCodecs.collection(ArrayList::new, CODEC, PAGE);
        public static final PacketCodec<RegistryByteBuf, List<Dto>> PIN_LIST = PacketCodecs.collection(ArrayList::new, CODEC, MAX_PINS);
    }

    /** server -> client: открыть экран летописи */
    public record Open() implements CustomPayload {
        public static final CustomPayload.Id<Open> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_open"));
        public static final PacketCodec<RegistryByteBuf, Open> CODEC = PacketCodec.unit(new Open());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** client -> server: страница записей (page с нуля, новые первыми); маски: 0 = без фильтра, бит 12 в kMask — события мира */
    public record PageReq(int page, int kMask, int tMask, boolean majorOnly, String query) implements CustomPayload {
        public static final CustomPayload.Id<PageReq> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_req"));
        public static final PacketCodec<RegistryByteBuf, PageReq> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, PageReq::page,
                PacketCodecs.VAR_INT, PageReq::kMask,
                PacketCodecs.VAR_INT, PageReq::tMask,
                PacketCodecs.BOOL, PageReq::majorOnly,
                PacketCodecs.string(40), PageReq::query,
                PageReq::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** server -> client: страница записей (page уже скорректирована сервером) */
    public record Page(int page, int pages, int total, List<Dto> entries) implements CustomPayload {
        public static final CustomPayload.Id<Page> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_page"));
        public static final PacketCodec<RegistryByteBuf, Page> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Page::page,
                PacketCodecs.VAR_INT, Page::pages,
                PacketCodecs.VAR_INT, Page::total,
                Dto.LIST, Page::entries,
                Page::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** client -> server: перелететь к месту записи */
    public record Go(long id) implements CustomPayload {
        public static final CustomPayload.Id<Go> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_go"));
        public static final PacketCodec<RegistryByteBuf, Go> CODEC = PacketCodec.tuple(PacketCodecs.VAR_LONG, Go::id, Go::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** client -> server: сводка по королевству */
    public record SumReq(String k) implements CustomPayload {
        public static final CustomPayload.Id<SumReq> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_sumreq"));
        public static final PacketCodec<RegistryByteBuf, SumReq> CODEC = PacketCodec.tuple(PacketCodecs.string(16), SumReq::k, SumReq::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** server -> client: сводка; строки с "#" в начале — заголовки разделов */
    public record Sum(String k, List<String> lines) implements CustomPayload {
        public static final CustomPayload.Id<Sum> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_sum"));
        public static final PacketCodec<RegistryByteBuf, Sum> CODEC = PacketCodec.tuple(
                PacketCodecs.string(16), Sum::k,
                PacketCodecs.collection(ArrayList::new, PacketCodecs.string(LINE), MAX_LINES), Sum::lines,
                Sum::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** client -> server: последние важные события с позицией для меток на карте */
    public record PinReq() implements CustomPayload {
        public static final CustomPayload.Id<PinReq> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_pinreq"));
        public static final PacketCodec<RegistryByteBuf, PinReq> CODEC = PacketCodec.unit(new PinReq());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** server -> client: метки для карты мира */
    public record Pins(List<Dto> entries) implements CustomPayload {
        public static final CustomPayload.Id<Pins> ID = new CustomPayload.Id<>(Identifier.of("godbox", "chron_pins"));
        public static final PacketCodec<RegistryByteBuf, Pins> CODEC = PacketCodec.tuple(Dto.PIN_LIST, Pins::entries, Pins::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
