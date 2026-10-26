package ai.modular.exp;

import ai.modular.gp.EvoParams;
import ai.modular.gp.GPEvolver;
import ai.modular.gp.GPNode;
import ai.modular.gp.GPSerial;
import ai.modular.gp.TreeEvolver;
import ai.modular.gp.score.ScoreEvolver;
import ai.modular.gp.score.ScoreNode;
import ai.modular.gp.score.ScoreSerial;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

import rts.units.UnitTypeTable;

public class RunExperiment {
    public static void main(String[] args) {
        Experiment.runQuietly(() -> run(args));
    }

    private static void run(String[] args) throws Exception {
        String bot  = args[0];
        if (!Arrays.asList(Experiment.BOTS).contains(bot)) {
            throw new IllegalArgumentException("Unknown bot '" + bot + "' (expected one of " + String.join(", ", Experiment.BOTS) + ")");
        }
        int first   = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int last    = args.length > 2 ? Integer.parseInt(args[2]) : Experiment.RUNS;
        String out  = args.length > 3 ? args[3] : Experiment.DEFAULT_OUT;
        int threads = args.length > 4 ? Integer.parseInt(args[4]) : Runtime.getRuntime().availableProcessors();

        EvoParams template = Experiment.params(bot);
        if (args.length > 5) template.generations = Integer.parseInt(args[5]);
        if (args.length > 6) template.popSize = Integer.parseInt(args[6]);

        File dir = new File(out, bot);
        dir.mkdirs();
        try {
            Files.write(new File(dir, "config.txt").toPath(),
                    ("bot               : " + bot + "\n" + Experiment.describe(template)).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignore) {
        }

        UnitTypeTable utt = new UnitTypeTable();
        System.out.println("Training " + bot + " runs " + first + ".." + last + " -> " + dir + "  (" + threads + " threads)");
        for (int run = first; run <= last; run++) {
            File treeFile = new File(dir, "run" + run + ".txt");
            if (treeFile.exists()) {
                System.out.println("run " + run + ": already done, skipping");
                continue;
            }
            System.out.println("--- " + bot + " run " + run + " ---");
            EvoParams p = Experiment.params(bot);
            p.generations = template.generations;
            p.popSize = template.popSize;
            p.threads = threads;
            File tmp = new File(dir, "run" + run + ".txt.tmp");
            long t0 = System.currentTimeMillis();
            TreeEvolver<?> engine;
            int size, depth;
            try (PrintWriter log = new PrintWriter(new FileWriter(new File(dir, "gens_run" + run + ".csv")))) {
                p.genLog = log;
                if (bot.equals("score")) {
                    ScoreEvolver ev = new ScoreEvolver(utt, Experiment.trainMapsCsv(), run, p);
                    ScoreNode best = ev.run();
                    ScoreSerial.save(best, tmp.getPath());
                    engine = ev.engine();
                    size = best.size();
                    depth = best.depth();
                } else {
                    GPEvolver ev = new GPEvolver(utt, Experiment.trainMapsCsv(), run, bot.equals("hybrid"), p);
                    GPNode best = ev.run();
                    GPSerial.save(best, tmp.getPath());
                    engine = ev.engine();
                    size = best.size();
                    depth = best.depth();
                }
            }
            double secs = (System.currentTimeMillis() - t0) / 1000.0;
            try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "run" + run + ".stats.csv")))) {
                w.println(Experiment.STATS_HEADER);
                w.printf(Locale.ROOT, "%d,%d,%.1f,%d,%d,%d,%s,%.4f,%.4f,%.4f,%d,%d%n", run, run, secs, p.generations,
                        p.popSize, threads, hostName(), engine.finalValidation, engine.finalPopMean, engine.finalPopSd, size, depth);
            }
            Files.move(tmp.toPath(), treeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("run %d done in %.1fs (validation %.3f, tree %d nodes / depth %d)%n",
                    run, secs, engine.finalValidation, size, depth);
        }
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName().replace(',', '_');
        } catch (Exception e) {
            return "unknown";
        }
    }
}
