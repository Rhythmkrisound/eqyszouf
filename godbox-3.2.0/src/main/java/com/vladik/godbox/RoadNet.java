package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Чистая геометрия дорог без Minecraft: грубый A* по рельефу, сглаживание трассы и профиль высот. */
final class RoadNet {
    static final int NONE = Integer.MIN_VALUE;

    private RoadNet() {}

    /** height — высота верха земли или NONE (чанк не загружен); pen — доплата за клетку, &lt;0 = путь закрыт. */
    interface Terrain {
        int height(int x, int z);

        double pen(int x, int z);
    }

    private static final class Node {
        final int gx, gz;
        double g, f;
        Node parent;

        Node(int gx, int gz) { this.gx = gx; this.gz = gz; }
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /** Опорные точки обхода препятствий (сетка шагом 6) или null, если бюджет узлов исчерпан. */
    static List<int[]> route(Terrain t, int ax, int az, int bx, int bz, int budget) {
        final int S = 6;
        double dx = bx - ax, dz = bz - az, len = Math.sqrt(dx * dx + dz * dz);
        List<int[]> out = new ArrayList<>();
        out.add(new int[]{ax, az});
        if (len < S * 2) { out.add(new int[]{bx, bz}); return out; }
        double ux = dx / len, uz = dz / len, corr = Math.max(30, len * 0.25);
        Map<Long, Node> seen = new HashMap<>();
        Map<Long, Integer> hc = new HashMap<>();
        Map<Long, Double> pc = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>((p, q) -> Double.compare(p.f, q.f));
        Node s0 = new Node(0, 0);
        seen.put(0L, s0);
        open.add(s0);
        Node goal = null;
        for (int ex = 0; ex < budget && !open.isEmpty(); ex++) {
            Node n = open.poll();
            int x = ax + n.gx * S, z = az + n.gz * S;
            if (Math.hypot(bx - x, bz - z) <= S * 1.5) { goal = n; break; }
            int h1 = cachedH(t, hc, x, z);
            for (int[] d : DIRS) {
                int gx = n.gx + d[0], gz = n.gz + d[1], px = ax + gx * S, pz = az + gz * S;
                double along = (px - ax) * ux + (pz - az) * uz, perp = Math.abs(-(px - ax) * uz + (pz - az) * ux);
                if (along < -S * 2 || along > len + S * 2 || perp > corr) continue;
                double L = S * Math.sqrt(d[0] * d[0] + d[1] * d[1]), pen = 0;
                boolean blocked = false;
                for (int k = 1; k <= 3 && !blocked; k++) {
                    int sx = x + (px - x) * k / 3, sz = z + (pz - z) * k / 3;
                    double p = pc.computeIfAbsent(BlockKey.of(sx, sz), q -> t.pen(sx, sz));
                    if (p < 0) blocked = true;
                    else pen += p / 3;
                }
                if (blocked) continue;
                int h2 = cachedH(t, hc, px, pz);
                double ratio = h1 == NONE || h2 == NONE ? 0 : Math.abs(h2 - h1) / L;
                double cost = L * (1 + pen + 6 * ratio * ratio) + (ratio > 0.8 ? L * 20 : 0);
                long key = BlockKey.of(gx, gz);
                Node o = seen.get(key);
                double g = n.g + cost;
                if (o != null && o.g <= g) continue;
                Node nn = new Node(gx, gz);
                nn.g = g;
                nn.f = g + 1.15 * Math.hypot(bx - px, bz - pz);
                nn.parent = n;
                seen.put(key, nn);
                open.add(nn);
            }
        }
        if (goal == null) return null;
        List<int[]> rev = new ArrayList<>();
        for (Node n = goal; n != null; n = n.parent) rev.add(new int[]{ax + n.gx * S, az + n.gz * S});
        Collections.reverse(rev);
        rev.set(0, new int[]{ax, az});
        if (rev.size() > 1 && Math.hypot(bx - rev.get(rev.size() - 1)[0], bz - rev.get(rev.size() - 1)[1]) < S * 0.75) rev.remove(rev.size() - 1);
        rev.add(new int[]{bx, bz});
        return rev;
    }

    private static int cachedH(Terrain t, Map<Long, Integer> c, int x, int z) {
        return c.computeIfAbsent(BlockKey.of(x, z), k -> t.height(x, z));
    }

    private static final class BlockKey {
        static long of(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    }

    /** Сглаживает ломаную (Chaikin) и растеризует в 8-связную цепочку клеток без дублей. */
    static List<int[]> smooth(List<int[]> wp) {
        List<double[]> p = new ArrayList<>();
        for (int[] q : wp) p.add(new double[]{q[0], q[1]});
        for (int it = 0; it < 2 && p.size() > 2; it++) {
            List<double[]> n = new ArrayList<>();
            n.add(p.get(0));
            for (int i = 0; i + 1 < p.size(); i++) {
                double[] a = p.get(i), b = p.get(i + 1);
                if (i > 0) n.add(new double[]{0.75 * a[0] + 0.25 * b[0], 0.75 * a[1] + 0.25 * b[1]});
                if (i + 2 < p.size()) n.add(new double[]{0.25 * a[0] + 0.75 * b[0], 0.25 * a[1] + 0.75 * b[1]});
            }
            n.add(p.get(p.size() - 1));
            p = n;
        }
        List<int[]> out = new ArrayList<>();
        int[] prev = null;
        for (double[] q : p) {
            int x = (int) Math.round(q[0]), z = (int) Math.round(q[1]);
            if (prev == null) { out.add(new int[]{x, z}); prev = out.get(0); continue; }
            line(out, prev[0], prev[1], x, z);
            prev = out.get(out.size() - 1);
        }
        return out;
    }

    private static void line(List<int[]> out, int x0, int z0, int x1, int z1) {
        int dx = Math.abs(x1 - x0), dz = Math.abs(z1 - z0), sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1, err = dx - dz, x = x0, z = z0;
        while (x != x1 || z != z1) {
            int e2 = 2 * err;
            if (e2 > -dz) { err -= dz; x += sx; }
            if (e2 < dx) { err += dx; z += sz; }
            out.add(new int[]{x, z});
        }
    }

    /**
     * Профиль высоты полотна для точек [from,to). raw[j] — высота верха земли (или готового моста) либо NONE;
     * water[j] — точка над водой (высота закреплена по уровню воды); deck[j] вне [from,to) — уже уложенные значения (закреплены).
     * Сглаживание скользящим средним, затем ограничение уклона: не больше 1 блока на 2 точки. false — нет данных.
     */
    static boolean solveDeck(int[] raw, boolean[] water, int[] deck, int from, int to, int look) {
        int n = raw.length, lo = Math.max(0, from - look), hi = Math.min(n, to + look), m = hi - lo;
        double[] v = new double[m];
        boolean[] known = new boolean[m], pin = new boolean[m];
        for (int j = lo; j < hi; j++) {
            int k = j - lo;
            boolean fixed = deck[j] != NONE && (j < from || j >= to);
            if (fixed) { v[k] = deck[j]; known[k] = true; pin[k] = true; }
            else if (raw[j] != NONE) { v[k] = raw[j]; known[k] = true; }
        }
        for (int j = lo; j < hi; ) {
            int k = j - lo;
            if (!water[j] || raw[j] == NONE || pin[k]) { j++; continue; }
            int e = j, top = raw[j];
            while (e < hi && water[e] && !pin[e - lo]) { if (raw[e] != NONE) top = Math.max(top, raw[e]); e++; }
            for (int q = j; q < e; q++) { v[q - lo] = top; known[q - lo] = true; pin[q - lo] = true; }
            j = e;
        }
        int any = -1;
        for (int k = 0; k < m; k++) if (known[k]) { any = k; break; }
        if (any < 0) return false;
        for (int k = 0; k < m; k++) {
            if (known[k]) continue;
            int a = k - 1, b = k + 1;
            while (a >= 0 && !known[a]) a--;
            while (b < m && !known[b]) b++;
            if (a < 0) v[k] = v[b];
            else if (b >= m) v[k] = v[a];
            else v[k] = v[a] + (v[b] - v[a]) * (k - a) / (double) (b - a);
        }
        double[] s = new double[m];
        for (int k = 0; k < m; k++) {
            if (pin[k]) { s[k] = v[k]; continue; }
            double sum = 0;
            int c = 0;
            for (int d = -4; d <= 4; d++) if (k + d >= 0 && k + d < m) { sum += v[k + d]; c++; }
            s[k] = Math.rint(sum / c);
        }
        for (int round = 0; round < 3; round++) {
            for (int k = 1; k < m; k++) if (!pin[k]) s[k] = clamp(s, k, -1, -2);
            for (int k = m - 2; k >= 0; k--) if (!pin[k]) s[k] = clamp(s, k, 1, 2);
        }
        for (int j = from; j < to; j++) deck[j] = (int) s[j - lo];
        return true;
    }

    private static double clamp(double[] s, int k, int d1, int d2) {
        double lo = -1e9, hi = 1e9;
        int m = s.length;
        for (int d : new int[]{d1, d2}) {
            int q = k + d;
            if (q < 0 || q >= m) continue;
            lo = Math.max(lo, s[q] - 1);
            hi = Math.min(hi, s[q] + 1);
        }
        return lo > hi ? s[k] : Math.max(lo, Math.min(hi, s[k]));
    }
}
