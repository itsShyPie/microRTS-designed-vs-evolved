package ai.modular.exp;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.modular.ModularBot;
import ai.modular.gp.GPBot;
import ai.modular.gp.GPNode;
import ai.modular.gp.GPSerial;
import ai.modular.gp.Panel;
import ai.modular.gp.Stats;
import ai.modular.gp.score.ScoreGPBot;
import ai.modular.gp.score.ScoreNode;
import ai.modular.gp.score.ScoreSerial;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class EvalExperiment {
    static final UnitTypeTable utt = new UnitTypeTable();

    public static void main(String[] args) {
        Experiment.runQuietly(() -> run(args));
    }

    private static void run(String[] args) throws Exception {
        String out   = args.length > 0 ? args[0] : Experiment.DEFAULT_OUT;
        int perSide  = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int threads  = args.length > 2 ? Integer.parseInt(args[2]) : Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        String[] bots = args.length > 3 ? args[3].split(",") : Experiment.BOTS;

        File cells = new File(out, "eval_cells.csv");
        Set<String> done = doneRuns(cells);
        if (!cells.exists()) {
            new File(out).mkdirs();
            try (PrintWriter w = new PrintWriter(new FileWriter(cells))) {
                w.println("bot,run,map,opponent,games,wins,draws,losses");
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            if (!done.contains("modular#0")) evaluate(pool, cells, "modular", 0, botFor(out, "modular", 0), perSide);
            for (String bot : bots) {
                for (int run : runIds(new File(out, bot))) {
                    if (done.contains(bot + "#" + run)) continue;
                    evaluate(pool, cells, bot, run, botFor(out, bot, run), perSide);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        writeSummary(out, bots);
    }

    static Supplier<AI> botFor(String out, String bot, int run) throws IOException {
        if (bot.equals("modular")) return () -> new ModularBot(utt, new AStarPathFinding());
        String path = new File(out, bot + "/run" + run + ".txt").getPath();
        if (bot.equals("score")) {
            ScoreNode s = ScoreSerial.load(path);
            return () -> new ScoreGPBot(utt, s.copy());
        }
        GPNode g = GPSerial.load(path);
        if (bot.equals("hybrid")) return () -> new ModularBot(utt, new AStarPathFinding(), false, g.copy());
        return () -> new GPBot(utt, new AStarPathFinding(), g.copy());
    }

    private static void evaluate(ExecutorService pool, File cells, String bot, int run, Supplier<AI> make, int perSide) throws Exception {
        long t0 = System.currentTimeMillis();
        String[] maps = Experiment.ALL_MAPS, opps = Experiment.ALL_OPPONENTS;
        int games = 2 * perSide;
        List<Future<Integer>> fs = new ArrayList<>();
        for (String map : maps)
            for (String opp : opps)
                for (int g = 0; g < games; g++) {
                    boolean botIsP0 = (g % 2 == 0);
                    fs.add(pool.submit(() -> play(make, opp, map, botIsP0)));
                }
        int k = 0, totalWins = 0;
        StringBuilder rows = new StringBuilder();
        for (String map : maps)
            for (String opp : opps) {
                int w = 0, d = 0, l = 0;
                for (int g = 0; g < games; g++) {
                    int res = fs.get(k++).get();
                    if (res > 0) w++; else if (res == 0) d++; else l++;
                }
                totalWins += w;
                rows.append(bot).append(',').append(run).append(',').append(Experiment.shortMap(map)).append(',')
                    .append(opp).append(',').append(games).append(',').append(w).append(',').append(d).append(',').append(l).append('\n');
            }
        try (PrintWriter w = new PrintWriter(new FileWriter(cells, true))) {
            w.print(rows);
        }
        System.out.printf("%-7s run %2d: win rate %.1f%% over %d games (%.0fs)%n", bot, run,
                100.0 * totalWins / (maps.length * opps.length * games), maps.length * opps.length * games,
                (System.currentTimeMillis() - t0) / 1000.0);
    }

    static int play(Supplier<AI> make, String oppName, String map, boolean botIsP0) throws Exception {
        GameState gs = new GameState(PhysicalGameState.load(map, utt), utt);
        AI bot = make.get();
        AI opp = Panel.make(oppName, utt);
        AI a0 = botIsP0 ? bot : opp;
        AI a1 = botIsP0 ? opp : bot;
        boolean over;
        do {
            gs.issueSafe(a0.getAction(0, gs));
            gs.issueSafe(a1.getAction(1, gs));
            over = gs.cycle();
        } while (!over && gs.getTime() < Experiment.MAX_CYCLES);
        int w = gs.winner();
        bot.gameOver(w);
        opp.gameOver(w);
        int me = botIsP0 ? 0 : 1;
        return w == -1 ? 0 : (w == me ? 1 : -1);
    }

    private static Set<String> doneRuns(File cells) throws IOException {
        Set<String> done = new HashSet<>();
        for (String[] r : readCsv(cells)) done.add(r[0] + "#" + r[1]);
        return done;
    }

    private static List<Integer> runIds(File dir) {
        List<Integer> ids = new ArrayList<>();
        String[] names = dir.list();
        if (names == null) return ids;
        for (String n : names) if (n.matches("run\\d+\\.txt")) ids.add(Integer.parseInt(n.replaceAll("\\D", "")));
        ids.sort(null);
        return ids;
    }

    private static double[] runStats(File botDir, int run) throws IOException {
        File f = new File(botDir, "run" + run + ".stats.csv");
        if (!f.exists()) f = new File(botDir, "runs.csv");
        if (!f.exists()) return null;
        try (BufferedReader b = new BufferedReader(new FileReader(f))) {
            List<String> head = java.util.Arrays.asList(b.readLine().split(","));
            int iRun = head.indexOf("run"), iSec = head.indexOf("train_seconds"), iSd = head.indexOf("final_pop_sd");
            double[] found = null;
            String line;
            while ((line = b.readLine()) != null) {
                String[] r = line.split(",");
                if (r.length > Math.max(iSec, iSd) && r[iRun].equals(String.valueOf(run))) {
                    found = new double[]{ Double.parseDouble(r[iSec]), Double.parseDouble(r[iSd]) };
                }
            }
            return found;
        }
    }

    private static List<String[]> readCsv(File f) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!f.exists()) return rows;
        try (BufferedReader b = new BufferedReader(new FileReader(f))) {
            String line = b.readLine();
            while ((line = b.readLine()) != null) if (!line.trim().isEmpty()) rows.add(line.split(","));
        }
        return rows;
    }

    static class RunResult {
        int run;
        double[] wg = new double[10];
        Map<String, double[]> byOpp = new LinkedHashMap<>();
        Map<String, double[]> byMap = new LinkedHashMap<>();
        List<Double> cellRates = new ArrayList<>();
        double trainSeconds = Double.NaN, finalPopSd = Double.NaN;

        double rate(int i) { return 100.0 * wg[2 * i] / wg[2 * i + 1]; }
    }

    private static Map<Integer, RunResult> collect(List<String[]> cells, String bot) {
        Map<Integer, RunResult> runs = new TreeMap<>();
        for (String[] c : cells) {
            if (!c[0].equals(bot)) continue;
            int run = Integer.parseInt(c[1]);
            String map = c[2], opp = c[3];
            double games = Double.parseDouble(c[4]), wins = Double.parseDouble(c[5]);
            RunResult r = runs.computeIfAbsent(run, k -> new RunResult());
            r.run = run;
            boolean heldOpp = Experiment.isHeldOutOpponent(opp);
            boolean testMap = false;
            for (String m : Experiment.TEST_MAPS) if (Experiment.shortMap(m).equals(map)) testMap = true;
            add(r.wg, 0, wins, games);
            add(r.wg, heldOpp ? 2 : 1, wins, games);
            add(r.wg, testMap ? 4 : 3, wins, games);
            add(r.byOpp.computeIfAbsent(opp, k -> new double[2]), 0, wins, games);
            add(r.byMap.computeIfAbsent(map, k -> new double[2]), 0, wins, games);
            r.cellRates.add(100.0 * wins / games);
        }
        return runs;
    }

    private static void add(double[] a, int i, double wins, double games) {
        a[2 * i] += wins;
        a[2 * i + 1] += games;
    }

    static void writeSummary(String out, String[] bots) throws IOException {
        List<String[]> cells = readCsv(new File(out, "eval_cells.csv"));
        Map<String, Map<Integer, RunResult>> all = new LinkedHashMap<>();
        all.put("modular", collect(cells, "modular"));
        for (String bot : bots) {
            Map<Integer, RunResult> runs = collect(cells, bot);
            for (Map.Entry<Integer, RunResult> e : runs.entrySet()) {
                double[] st = runStats(new File(out, bot), e.getKey());
                if (st != null) {
                    e.getValue().trainSeconds = st[0];
                    e.getValue().finalPopSd = st[1];
                }
            }
            all.put(bot, runs);
        }

        StringBuilder md = new StringBuilder();
        md.append("# Shared experiment results\n\n```\n").append(Experiment.describe(Experiment.params("gp"))).append("```\n\n");
        md.append("Win rate = games won / games played (draws, including the 2000-cycle cap, count as not won). ")
          .append("Opponents marked * are held out of training; maps marked * are held out of training.\n\n");

        md.append("## Overall (mean ± SD across runs)\n\n")
          .append("| Bot | Runs | Win rate | Train opps | Held-out opps* | Train maps | Test maps* | SD per run | SD over population | Training time |\n")
          .append("|---|---|---|---|---|---|---|---|---|---|\n");
        try (PrintWriter csv = new PrintWriter(new FileWriter(new File(out, "summary_runs.csv")))) {
            csv.println("bot,run,win_rate,train_opp,held_out_opp,train_map,test_map,sd_per_run,train_seconds,final_pop_sd");
            for (Map.Entry<String, Map<Integer, RunResult>> e : all.entrySet()) {
                List<RunResult> rs = new ArrayList<>(e.getValue().values());
                if (rs.isEmpty()) continue;
                double[][] col = new double[5][rs.size()];
                double[] sdRun = new double[rs.size()];
                List<Double> secs = new ArrayList<>(), popSd = new ArrayList<>();
                for (int i = 0; i < rs.size(); i++) {
                    RunResult r = rs.get(i);
                    for (int c = 0; c < 5; c++) col[c][i] = r.rate(c);
                    sdRun[i] = Stats.sd(toArray(r.cellRates));
                    if (!Double.isNaN(r.trainSeconds)) secs.add(r.trainSeconds);
                    if (!Double.isNaN(r.finalPopSd)) popSd.add(r.finalPopSd);
                    csv.printf(Locale.ROOT, "%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f,%.4f%n", e.getKey(), r.run, col[0][i], col[1][i],
                            col[2][i], col[3][i], col[4][i], sdRun[i], r.trainSeconds, r.finalPopSd);
                }
                md.append("| ").append(e.getKey()).append(" | ").append(rs.size());
                for (int c = 0; c < 5; c++) md.append(" | ").append(pm(col[c]));
                md.append(" | ").append(String.format(Locale.ROOT, "%.1f", Stats.mean(sdRun)));
                md.append(" | ").append(popSd.isEmpty() ? "n/a" : String.format(Locale.ROOT, "%.3f ± %.3f", Stats.mean(toArray(popSd)), Stats.sd(toArray(popSd))));
                md.append(" | ").append(secs.isEmpty() ? "n/a" : String.format(Locale.ROOT, "%.0f ± %.0f s (total %.1f h)",
                        Stats.mean(toArray(secs)), Stats.sd(toArray(secs)), Stats.mean(toArray(secs)) * secs.size() / 3600));
                md.append(" |\n");
            }
        }
        md.append("\n- **SD per run**: standard deviation of a run's win rate across the 90 map × opponent cells, averaged over runs.\n")
          .append("- **SD over population**: standard deviation of fitness across the final population (per generation in `gens_runK.csv`).\n")
          .append("- **± SD**: standard deviation of the per-run value across runs.\n\n");

        md.append("## Per opponent (win rate, mean ± SD across runs)\n\n");
        table(md, all, true);
        md.append("\n## Per map (win rate, mean ± SD across runs)\n\n");
        table(md, all, false);

        md.append("\n## Mann-Whitney U between evolved bots (per-run win rates)\n\n| Comparison | Measure | U | p |\n|---|---|---|---|\n");
        for (int a = 0; a < bots.length; a++)
            for (int b = a + 1; b < bots.length; b++) {
                List<RunResult> ra = new ArrayList<>(all.get(bots[a]).values()), rb = new ArrayList<>(all.get(bots[b]).values());
                if (ra.size() < 2 || rb.size() < 2) continue;
                for (int c : new int[]{ 0, 2, 4 }) {
                    double[] va = new double[ra.size()], vb = new double[rb.size()];
                    for (int i = 0; i < va.length; i++) va[i] = ra.get(i).rate(c);
                    for (int i = 0; i < vb.length; i++) vb[i] = rb.get(i).rate(c);
                    double u = Stats.mannWhitneyU(va, vb);
                    md.append(String.format(Locale.ROOT, "| %s vs %s | %s | %.1f | %.3f |%n", bots[a], bots[b],
                            c == 0 ? "overall" : c == 2 ? "held-out opps" : "test maps", u, Stats.approxP(u, va.length, vb.length)));
                }
            }

        try (PrintWriter w = new PrintWriter(new File(out, "summary.md"), "UTF-8")) {
            w.print(md);
        }
        System.out.println("\nWrote " + new File(out, "summary.md").getPath());
    }

    private static void table(StringBuilder md, Map<String, Map<Integer, RunResult>> all, boolean byOpp) {
        List<String> keys = new ArrayList<>();
        if (byOpp) for (String o : Experiment.ALL_OPPONENTS) keys.add(o);
        else for (String m : Experiment.ALL_MAPS) keys.add(Experiment.shortMap(m));
        md.append("| Bot");
        for (String k : keys) {
            boolean held = byOpp ? Experiment.isHeldOutOpponent(k) : isTestShort(k);
            md.append(" | ").append(k).append(held ? "*" : "");
        }
        md.append(" |\n|---");
        for (int i = 0; i < keys.size(); i++) md.append("|---");
        md.append("|\n");
        for (Map.Entry<String, Map<Integer, RunResult>> e : all.entrySet()) {
            List<RunResult> rs = new ArrayList<>(e.getValue().values());
            if (rs.isEmpty()) continue;
            md.append("| ").append(e.getKey());
            for (String k : keys) {
                double[] v = new double[rs.size()];
                for (int i = 0; i < rs.size(); i++) {
                    double[] wg = (byOpp ? rs.get(i).byOpp : rs.get(i).byMap).get(k);
                    v[i] = wg == null ? Double.NaN : 100.0 * wg[0] / wg[1];
                }
                md.append(" | ").append(pm(v));
            }
            md.append(" |\n");
        }
    }

    private static boolean isTestShort(String shortMap) {
        for (String m : Experiment.TEST_MAPS) if (Experiment.shortMap(m).equals(shortMap)) return true;
        return false;
    }

    private static String pm(double[] v) {
        return v.length < 2 ? String.format(Locale.ROOT, "%.1f%%", Stats.mean(v)) : String.format(Locale.ROOT, "%.1f ± %.1f%%", Stats.mean(v), Stats.sd(v));
    }

    private static double[] toArray(List<Double> l) {
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }
}
