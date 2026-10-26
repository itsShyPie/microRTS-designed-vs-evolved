package ai.modular.gp.score;

import ai.modular.gp.EvoParams;
import ai.modular.gp.TreeEvolver;

import rts.units.UnitTypeTable;

public class ScoreEvolver {
    private final TreeEvolver<ScoreNode> ev;

    public ScoreEvolver(UnitTypeTable utt, String mapsCsv, long seed) {
        this(utt, mapsCsv, seed, EvoParams.v1(ScoreConfig.MAX_DEPTH, ScoreConfig.MAX_NODES));
    }

    public ScoreEvolver(UnitTypeTable utt, String mapsCsv, long seed, EvoParams p) {
        ev = new TreeEvolver<>(utt, mapsCsv.split(","), seed, p,
                r -> ScoreNode.random(r, p.initDepth),
                t -> new ScoreGPBot(utt, t.copy()));
    }

    public ScoreNode run() throws Exception {
        return ev.run();
    }

    public double validateAllMaps(ScoreNode tree, int gamesPer) throws Exception {
        return ev.validateAllMaps(tree, gamesPer);
    }

    public TreeEvolver<ScoreNode> engine() {
        return ev;
    }
}
