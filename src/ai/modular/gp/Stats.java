package ai.modular.gp;

import java.util.Arrays;

public final class Stats {
    private Stats() {}

    public static double mean(double[] v) {
        if (v.length == 0) return Double.NaN;
        double s = 0;
        for (double x : v) s += x;
        return s / v.length;
    }

    public static double sd(double[] v) {
        if (v.length < 2) return 0.0;
        double m = mean(v), ss = 0;
        for (double x : v) ss += (x - m) * (x - m);
        return Math.sqrt(ss / (v.length - 1));
    }

    public static double mannWhitneyU(double[] a, double[] b) {
        int na = a.length, nb = b.length;
        double[] all = new double[na + nb];
        System.arraycopy(a, 0, all, 0, na);
        System.arraycopy(b, 0, all, na, nb);
        double[] ranks = ranks(all);
        double ra = 0;
        for (int i = 0; i < na; i++) ra += ranks[i];
        double ua = ra - na * (na + 1) / 2.0;
        double ub = (double) na * nb - ua;
        return Math.min(ua, ub);
    }

    private static double[] ranks(double[] v) {
        int n = v.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (x, y) -> Double.compare(v[x], v[y]));
        double[] r = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && v[idx[j + 1]] == v[idx[i]]) j++;
            double avg = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) r[idx[k]] = avg;
            i = j + 1;
        }
        return r;
    }

    public static double approxP(double u, int na, int nb) {
        double mu = na * nb / 2.0;
        double sigma = Math.sqrt(na * nb * (na + nb + 1) / 12.0);
        if (sigma == 0) return 1.0;
        double z = (u - mu) / sigma;
        return 2 * (1 - phi(Math.abs(z)));
    }

    private static double phi(double x) { return 0.5 * (1 + erf(x / Math.sqrt(2))); }

    private static double erf(double x) {
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
        double y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return x >= 0 ? y : -y;
    }
}
