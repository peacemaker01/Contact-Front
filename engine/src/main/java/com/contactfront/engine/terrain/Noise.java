package com.contactfront.engine.terrain;

public final class Noise {
    private final long seed;

    public Noise(long seed) {
        this.seed = seed;
    }

    private long hash(int x, int y) {
        long h = seed + x * 374761393L + y * 668265263L;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h = h ^ (h >>> 31);
        return h & 0x7fffffffffffffffL;
    }

    private double val(int x, int y) {
        return (hash(x, y) / (double) 0x7fffffffffffffffL);
    }

    public double noise(double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double sx = fx * fx * (3 - 2 * fx);
        double sy = fy * fy * (3 - 2 * fy);
        double n00 = val(x0, y0);
        double n10 = val(x0 + 1, y0);
        double n01 = val(x0, y0 + 1);
        double n11 = val(x0 + 1, y0 + 1);
        double ix0 = n00 + (n10 - n00) * sx;
        double ix1 = n01 + (n11 - n01) * sx;
        return ix0 + (ix1 - ix0) * sy;
    }

    public double fbm(double x, double y, int octaves, int scale) {
        double amp = 1, freq = 1, sum = 0, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise(x * freq / (double) scale, y * freq / (double) scale);
            norm += amp;
            amp *= 0.5;
            freq *= 2;
        }
        return sum / norm;
    }
}
