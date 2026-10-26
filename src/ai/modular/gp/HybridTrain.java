package ai.modular.gp;

import rts.units.UnitTypeTable;

public class HybridTrain {
    public static void main(String[] args) throws Exception {
        String mapsCsv = args.length > 0 ? args[0]
            : "maps/8x8/basesWorkers8x8.xml,maps/12x12/basesWorkers12x12.xml,maps/16x16/basesWorkers16x16.xml";
        int seeds  = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        String out = args.length > 2 ? args[2] : "hybrid_best.txt";
        int first  = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        UnitTypeTable utt = new UnitTypeTable();
        System.out.println("Training HYBRID (scripted macro + evolved combat) on [" + mapsCsv
                + "] across " + seeds + " seed(s) ...");
        long t0 = System.currentTimeMillis();

        GPNode best = null;
        double bestVal = -1, sum = 0;
        for (int s = 1; s <= seeds; s++) {
            boolean reload = s < first;
            System.out.println("--- seed " + s + (reload ? " (reloaded, not retrained) ---" : " ---"));
            GPEvolver ev = new GPEvolver(utt, mapsCsv, s, true);
            GPNode tree = reload ? GPSerial.load("hybrid_seed" + s + ".txt") : ev.run();
            double val = ev.validateAllMaps(tree, 6);
            if (!reload) GPSerial.save(tree, "hybrid_seed" + s + ".txt");
            sum += val;
            System.out.printf("seed %d validation winrate = %.1f%%%n", s, val * 100);
            if (val > bestVal) { bestVal = val; best = tree; }
        }

        GPSerial.save(best, out);
        System.out.printf("%nSeeds: %d   mean validation = %.1f%%   best = %.1f%%%n",
                seeds, 100.0 * sum / seeds, bestVal * 100);
        System.out.printf("Done in %.1fs. Saved best combat tree to %s%n",
                (System.currentTimeMillis() - t0) / 1000.0, out);
    }
}
