"""Значки сезонов 16x16 (64x16): росток, солнце, лист, снежинка. Рисуется кодом, оригинальный пиксель-арт.

Запуск: python3 tools/ui/seasons.py  ->  assets/godbox/textures/gui/seasons.png и /tmp/verify/seasons.png
"""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "src/main/resources/assets/godbox/textures/gui/seasons.png")

PAL = {
    ".": (0, 0, 0, 0),
    "o": (24, 30, 22, 255),      # контур
    "g": (96, 190, 70, 255),     # листья
    "G": (58, 140, 50, 255),
    "b": (120, 82, 50, 255),     # земля
    "B": (86, 58, 36, 255),
    "y": (255, 214, 64, 255),    # солнце
    "Y": (255, 160, 30, 255),
    "w": (255, 244, 170, 255),
    "r": (214, 84, 30, 255),     # осенний лист
    "R": (160, 52, 24, 255),
    "a": (236, 150, 40, 255),
    "i": (225, 242, 255, 255),   # снег
    "I": (140, 190, 235, 255),
    "d": (90, 140, 200, 255),
}

SPRING = [
    "................",
    "................",
    "....ooo.........",
    "...oggGo...ooo..",
    "..oggggGo.oggGo.",
    "..oggggGoooggGo.",
    "...oGGGoGoGGGo..",
    "....ooo.oGo oo..".replace(" ", "."),
    ".........oGo....",
    ".........oGo....",
    ".........oGo....",
    "....ooooooGoooo.",
    "...obbbbbbGbbbo.",
    "...obBbbbbbbBbo.",
    "....oooooooooo..",
    "................",
]

SUMMER = [
    "................",
    ".......o........",
    "...o...o...o....",
    "....o.....o.....",
    ".....ooooo......",
    "...o.oyyyyo.o...",
    "....oyywwyyo....",
    ".ooooywwwyYoooo.",
    "....oyywwyYo....",
    "...o.oyyYYo.o...",
    ".....ooooo......",
    "....o.....o.....",
    "...o...o...o....",
    ".......o........",
    "................",
    "................",
]

AUTUMN = [
    "................",
    "................",
    "..........oo....",
    "........ooaao...",
    "......ooaaaro...",
    "....ooaaarrro...",
    "...oaaarRrrro...",
    "..oaaarRrrro....",
    "..oaarRrrro.....",
    "..oarRrro.......",
    "..orRro.........",
    ".oRrooo.........",
    "oRoo............",
    "oo..............",
    "................",
    "................",
]

WINTER = [
    "................",
    ".......oo.......",
    "......oIio......",
    "...o..oIio..o...",
    "..oIo.oIio.oIo..",
    "...oIoodIooIo...",
    "....oIIiiIIo....",
    ".ooooddiiddoooo.",
    ".oIIIIiiiiIIIIo.",
    ".ooooddiiddoooo.",
    "....oIIiiIIo....",
    "...oIoodIooIo...",
    "..oIo.oIio.oIo..",
    "...o..oIio..o...",
    "......oIio......",
    ".......oo.......",
]


def draw(img, ox, rows):
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            img.putpixel((ox + x, y), PAL[ch])


def main():
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    for i, rows in enumerate((SPRING, SUMMER, AUTUMN, WINTER)):
        draw(img, i * 16, rows)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    os.makedirs("/tmp/verify", exist_ok=True)
    sheet = Image.new("RGBA", (64 * 8, 16 * 8), (60, 64, 72, 255))
    sheet.alpha_composite(img.resize((64 * 8, 16 * 8), Image.NEAREST))
    sheet.save("/tmp/verify/seasons.png")


if __name__ == "__main__":
    main()
