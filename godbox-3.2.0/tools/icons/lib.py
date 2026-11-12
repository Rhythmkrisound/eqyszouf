"""Ядро генератора иконок: слой с ролями цветов, палитры, контур, значки, мини-шрифт."""

OUTLINE = (30, 22, 40, 255)

# Фиксированные цвета (символ в спрайте -> RGB).
FIXED = {
    'w': (248, 248, 252), 'W': (205, 212, 224), 'g': (150, 158, 174), 'G': (92, 98, 116), 'k': (40, 34, 52),
    'y': (255, 226, 92), 'Y': (226, 164, 40), 'o': (255, 150, 44), 'r': (230, 62, 52), 'R': (152, 30, 44),
    'b': (74, 124, 232), 'c': (126, 224, 244), 'B': (134, 88, 52), 'D': (84, 54, 36),
    'n': (96, 202, 72), 'N': (42, 124, 62), 'p': (176, 94, 222), 'P': (102, 52, 152),
    'l': (255, 154, 192), 'f': (250, 204, 164), 'F': (214, 150, 110), 'a': (46, 190, 170), 'A': (24, 112, 112),
    'e': (70, 224, 120), 't': (232, 214, 170), 'T': (190, 164, 110),
}

# Рампы: (светлый, средний, тёмный) — свет сверху-слева.
RAMPS = {
    'black': ((132, 132, 156), (80, 78, 102), (46, 42, 64)),
    'gray': ((214, 220, 232), (156, 164, 182), (98, 104, 124)),
    'white': ((255, 255, 255), (226, 232, 242), (170, 180, 200)),
    'red': ((255, 120, 100), (226, 60, 52), (146, 28, 44)),
    'orange': ((255, 206, 90), (255, 148, 44), (196, 84, 36)),
    'fire': ((255, 232, 110), (255, 150, 44), (216, 60, 40)),
    'yellow': ((255, 246, 150), (255, 214, 66), (212, 150, 36)),
    'gold': ((255, 240, 140), (240, 190, 50), (178, 118, 32)),
    'green': ((150, 230, 100), (80, 184, 70), (36, 112, 58)),
    'lime': ((210, 250, 120), (150, 220, 70), (80, 160, 56)),
    'blue': ((140, 190, 255), (70, 120, 232), (40, 70, 168)),
    'cyan': ((200, 248, 255), (110, 216, 240), (60, 150, 200)),
    'ice': ((236, 252, 255), (170, 226, 250), (104, 164, 224)),
    'purple': ((220, 150, 255), (160, 82, 216), (96, 46, 150)),
    'pink': ((255, 206, 224), (255, 140, 182), (206, 84, 132)),
    'brown': ((190, 140, 92), (134, 88, 52), (84, 54, 36)),
    'tan': ((246, 230, 186), (222, 196, 140), (168, 138, 92)),
    'skin': ((255, 224, 190), (240, 186, 146), (200, 138, 102)),
    'teal': ((120, 236, 214), (46, 176, 160), (24, 100, 110)),
    'dkgreen': ((90, 170, 90), (46, 120, 62), (28, 76, 48)),
    'lava': ((255, 224, 96), (255, 120, 32), (190, 50, 30)),
    'slime': ((190, 255, 150), (110, 220, 90), (56, 150, 66)),
    'stone': ((178, 182, 194), (128, 132, 148), (84, 88, 104)),
    'sand': ((255, 236, 170), (234, 200, 120), (190, 150, 84)),
    'flesh': ((255, 170, 170), (214, 96, 118), (140, 48, 82)),
    'violet': ((200, 160, 255), (120, 80, 210), (66, 40, 130)),
    'sculk': ((90, 230, 220), (22, 82, 98), (12, 40, 58)),
    'steel': ((226, 232, 242), (176, 186, 204), (118, 128, 152)),
    'rust': ((236, 160, 110), (190, 100, 64), (122, 58, 46)),
}


def hexs(rgb):
    return tuple(rgb) + (255,)


class Scheme:
    def __init__(self, p='gray', s='gray'):
        self.map = dict((k, hexs(v)) for k, v in FIXED.items())
        for i, ch in enumerate('hms'):
            self.map[ch] = hexs(RAMPS[p][i])
        for i, ch in enumerate('HMS'):
            self.map[ch] = hexs(RAMPS[s][i])

    def rgba(self, ch):
        return self.map[ch]


class Layer:
    def __init__(self):
        self.g = [[None] * 16 for _ in range(16)]

    def px(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < 16 and 0 <= y < 16 and c not in ('.', ' '):
            self.g[y][x] = c

    def clear(self, x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            self.g[y][x] = None

    def art(self, x, y, rows, flip=False):
        for j, row in enumerate(rows):
            if flip:
                row = row[::-1]
            for i, ch in enumerate(row):
                self.px(x + i, y + j, ch)

    def rect(self, x, y, w, h, c):
        for j in range(h):
            for i in range(w):
                self.px(x + i, y + j, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.px(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def thick(self, x0, y0, x1, y1, c1, c2=None):
        self.line(x0 + 1, y0, x1 + 1, y1, c2 or c1)
        self.line(x0, y0, x1, y1, c1)

    def ellipse(self, cx, cy, rx, ry, ramp='hms', light=True):
        """Закрашенный эллипс с освещением сверху-слева."""
        for y in range(16):
            for x in range(16):
                dx = (x + 0.5 - cx) / rx
                dy = (y + 0.5 - cy) / ry
                if dx * dx + dy * dy <= 1.0:
                    v = (dx + dy) * 0.5
                    if not light:
                        self.px(x, y, ramp[1])
                    elif dx * dx + dy * dy > 0.5 and v < -0.28:
                        self.px(x, y, ramp[0])
                    elif v > 0.3:
                        self.px(x, y, ramp[2])
                    else:
                        self.px(x, y, ramp[1])

    def disc(self, cx, cy, r, ramp='hms'):
        self.ellipse(cx, cy, r, r, ramp)

    def shift(self, dx, dy):
        n = Layer()
        for y in range(16):
            for x in range(16):
                if self.g[y][x]:
                    n.px(x + dx, y + dy, self.g[y][x])
        self.g = n.g

    def mirror(self):
        for row in self.g:
            row.reverse()

    def opaque(self):
        return [(x, y) for y in range(16) for x in range(16) if self.g[y][x]]


def render(layers, scheme):
    """Слои снизу вверх; у каждого слоя свой контур. Возвращает 16x16 RGBA (список строк)."""
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for L in layers:
        for y in range(16):
            for x in range(16):
                if L.g[y][x] is None:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < 16 and 0 <= ny < 16 and L.g[ny][nx]:
                            img[y][x] = OUTLINE
                            break
        for y in range(16):
            for x in range(16):
                if L.g[y][x]:
                    img[y][x] = scheme.rgba(L.g[y][x])
    return img


# Мини-шрифт 3x5 для цифр на иконках времени.
FONT = {
    'x': ["...", "w.w", ".w.", "w.w", "..."],
    '0': ["www", "w.w", "w.w", "w.w", "www"],
    '1': [".w.", "ww.", ".w.", ".w.", "www"],
    '2': ["www", "..w", "www", "w..", "www"],
    '5': ["www", "w..", "www", "..w", "www"],
    '.': ["...", "...", "...", "...", "w.."],
}


def text(L, x, y, s, c='w'):
    for ch in s:
        for j, row in enumerate(FONT[ch]):
            for i, v in enumerate(row):
                if v == 'w':
                    L.px(x + i, y + j, c)
        x += 4 if ch != '.' else 2
