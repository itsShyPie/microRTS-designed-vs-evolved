package ai.modular.gp;

import ai.core.AI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class TreeEvolver<N extends EvoTree<N>> {
    private final UnitTypeTable utt;
    private final String[] trainMaps;
    private final Random r;
    private final EvoParams p;
    private final Function<Random, N> randomTree;
    private final Function<N, AI> makeBot;
    private final List<N> scratch = new ArrayList<>();
    private ExecutorService pool;

    public double finalValidation, finalPopMean, finalPopSd;

    public TreeEvolver(UnitTypeTable utt, String[] trainMaps, long seed, EvoParams p,
                       Function<Random, N> randomTree, Function<N, AI> makeBot) {
        this.utt = utt;
        this.trainMaps = trainMaps;
        this.r = new Random(seed);
        this.p = p;
        this.randomTree = randomTree;
        this.makeBot = makeBot;
    }

    public N run() throws Exception {
        if (p.threads > 1) pool = Executors.newFixedThreadPool(p.threads);
        try {
            return evolve();
        } finally {
            if (pool != null) pool.shutdownNow();
            pool = null;
        }
    }

    private N evolve() throws Exception {
        List<N> pop = new ArrayList<>();
        for (int i = 0; i < p.popSize; i++) pop.add(randomTree.apply(r));
        if (p.genLog != null) p.genLog.println("gen,map,best,mean,sd,best_size,best_depth,mean_size,seconds");

        for (int gen = 0; gen < p.generations; gen++) {
            long t0 = System.currentTimeMillis();
            String map = trainMaps[gen % trainMaps.length];
            double[] fit = fitnessAll(pop, map);

            Integer[] order = sortedDesc(fit);
            double[] ms = meanSd(fit);
            N best = pop.get(order[0]);
            System.out.printf("gen %2d  best=%.3f  avg=%.3f  sd=%.3f  bestSize=%d  map=%s%n",
                    gen, fit[order[0]], ms[0], ms[1], best.size(), shortMap(map));
            if (p.genLog != null) {
                double meanSize = 0;
                for (N t : pop) meanSize += t.size() / (double) pop.size();
                p.genLog.printf(Locale.ROOT, "%d,%s,%.4f,%.4f,%.4f,%d,%d,%.1f,%.1f%n", gen, shortMap(map), fit[order[0]],
                        ms[0], ms[1], best.size(), best.depth(), meanSize, (System.currentTimeMillis() - t0) / 1000.0);
                p.genLog.flush();
            }

            List<N> next = new ArrayList<>();
            int elite = Math.max(1, (int) (p.eliteFrac * p.popSize));
            for (int e = 0; e < elite; e++) next.add(pop.get(order[e]).copy());
            while (next.size() < p.popSize) {
                N parent, child;
                if (r.nextDouble() < p.crossoverRate) {
                    parent = pop.get(select(fit));
                    child = crossover(parent, pop.get(select(fit)));
                } else {
                    parent = pop.get(select(fit));
                    child = parent.copy();
                }
                if (r.nextDouble() < p.mutationRate) child.mutateInPlace(r, scratch);
                if (child.size() > p.maxNodes || child.depth() > p.maxDepth) {
                    child = (p.rejectToParent ? parent : pop.get(order[0])).copy();
                }
                next.add(child);
            }
            pop = next;
        }

        double[] finalFit = fitnessAll(pop, trainMaps[0]);
        double[] ms = meanSd(finalFit);
        finalPopMean = ms[0];
        finalPopSd = ms[1];
        Integer[] order = sortedDesc(finalFit);
        int k = Math.min(p.finalCandidates, p.popSize);
        double[] val = new double[k];
        if (pool == null) {
            for (int i = 0; i < k; i++) val[i] = validateAllMaps(pop.get(order[i]), p.finalValidationGames);
        } else {
            List<Future<Double>> fs = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                N cand = pop.get(order[i]);
                fs.add(pool.submit(() -> validateAllMaps(cand, p.finalValidationGames)));
            }
            for (int i = 0; i < k; i++) val[i] = fs.get(i).get();
        }
        N best = pop.get(order[0]);
        double bestVal = -1;
        for (int i = 0; i < k; i++) {
            if (val[i] > bestVal) { bestVal = val[i]; best = pop.get(order[i]); }
        }
        finalValidation = bestVal;
        System.out.printf("FINAL cross-map validation of chosen individual = %.3f%n", bestVal);
        return best;
    }

    private double[] fitnessAll(List<N> pop, String map) throws Exception {
        double[] fit = new double[pop.size()];
        if (pool == null) {
            for (int i = 0; i < pop.size(); i++) fit[i] = fitness(pop.get(i), map);
            return fit;
        }
        List<Future<Double>> fs = new ArrayList<>();
        for (N t : pop) fs.add(pool.submit(() -> fitness(t, map)));
        for (int i = 0; i < fit.length; i++) fit[i] = fs.get(i).get();
        return fit;
    }

    private N crossover(N p1, N p2) {
        N child = p1.copy();
        List<N> n1 = new ArrayList<>();
        child.collect(n1);
        List<N> n2 = new ArrayList<>();
        p2.collect(n2);
        N target = n1.get(r.nextInt(n1.size()));
        N donor = n2.get(r.nextInt(n2.size())).copy();
        target.copyFieldsFrom(donor);
        return child;
    }

    private int select(double[] fit) {
        int best = r.nextInt(fit.length);
        for (int i = 1; i < p.tournament; i++) {
            int c = r.nextInt(fit.length);
            if (fit[c] > fit[best]) best = c;
        }
        return best;
    }

    private Integer[] sortedDesc(final double[] fit) {
        Integer[] idx = new Integer[fit.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(fit[b], fit[a]));
        return idx;
    }

    private static double[] meanSd(double[] v) {
        double m = 0;
        for (double x : v) m += x / v.length;
        double ss = 0;
        for (double x : v) ss += (x - m) * (x - m);
        return new double[]{ m, Math.sqrt(ss / v.length) };
    }

    private double fitness(N tree, String map) throws Exception {
        double score = 0;
        int total = 0;
        for (int o = 0; o < p.trainOpponents.length; o++) {
            for (int g = 0; g < p.fitnessGames[o]; g++) {
                score += playShaped(tree, (g % 2 == 0), Panel.make(p.trainOpponents[o], utt), map);
                total++;
            }
        }
        return score / total;
    }

    public double validateAllMaps(N tree, int gamesPer) throws Exception {
        int wins = 0, total = 0;
        for (String m : trainMaps) {
            for (String opp : p.trainOpponents) {
                for (int g = 0; g < gamesPer; g++) {
                    boolean botIsP0 = (g % 2 == 0);
                    if (playWinner(tree, botIsP0, Panel.make(opp, utt), m) == (botIsP0 ? 0 : 1)) wins++;
                    total++;
                }
            }
        }
        return (double) wins / total;
    }

    private double playShaped(N tree, boolean botIsP0, AI opp, String map) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = makeBot.apply(tree);
        AI a0 = botIsP0 ? bot : opp;
        AI a1 = botIsP0 ? opp : bot;
        int me = botIsP0 ? 0 : 1;

        int peakWorkers = 0, peakArmy = 0, sample = 0;
        boolean over = false;
        do {
            gs.issueSafe(a0.getAction(0, gs));
            gs.issueSafe(a1.getAction(1, gs));
            over = gs.cycle();
            if ((sample++ % 50) == 0) {
                int[] c = counts(gs.getPhysicalGameState(), me);
                if (c[0] > peakWorkers) peakWorkers = c[0];
                if (c[1] > peakArmy) peakArmy = c[1];
            }
        } while (!over && gs.getTime() < p.maxCycles);

        int w = gs.winner();
        double outcome = (w == me) ? 1.0 : (w == -1 ? 0.4 : 0.0);
        double econ = Math.min(1.0, peakWorkers / 4.0);
        double mil  = Math.min(1.0, peakArmy / 6.0);
        return 0.7 * outcome + 0.18 * mil + 0.12 * econ;
    }

    private int playWinner(N tree, boolean botIsP0, AI opp, String map) throws Exception {
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);
        GameState gs = new GameState(pgs, utt);
        AI bot = makeBot.apply(tree);
        AI a0 = botIsP0 ? bot : opp;
        AI a1 = botIsP0 ? opp : bot;
        boolean over = false;
        do {
            gs.issueSafe(a0.getAction(0, gs));
            gs.issueSafe(a1.getAction(1, gs));
            over = gs.cycle();
        } while (!over && gs.getTime() < p.maxCycles);
        return gs.winner();
    }

    private int[] counts(PhysicalGameState pgs, int player) {
        int workers = 0, army = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (u.getType().canHarvest) workers++;
            else if (u.getType().canAttack) army++;
        }
        return new int[]{ workers, army };
    }

    private static String shortMap(String m) {
        int i = m.lastIndexOf('/');
        return i >= 0 ? m.substring(i + 1) : m;
    }
}
