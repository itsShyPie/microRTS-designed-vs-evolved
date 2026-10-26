package ai.modular.gp;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.modular.ModularBot;

import rts.units.UnitTypeTable;

public class GPEvolver {
    private final TreeEvolver<GPNode> ev;

    public GPEvolver(UnitTypeTable utt, String mapsCsv, long seed) {
        this(utt, mapsCsv, seed, false);
    }

    public GPEvolver(UnitTypeTable utt, String mapsCsv, long seed, boolean hybrid) {
        this(utt, mapsCsv, seed, hybrid, EvoParams.v1(GPConfig.MAX_DEPTH, GPConfig.MAX_NODES));
    }

    public GPEvolver(UnitTypeTable utt, String mapsCsv, long seed, boolean hybrid, EvoParams p) {
        ev = new TreeEvolver<>(utt, mapsCsv.split(","), seed, p,
                r -> GPNode.random(r, p.initDepth),
                t -> hybrid
                        ? new ModularBot(utt, new AStarPathFinding(), false, t.copy())
                        : new GPBot(utt, new AStarPathFinding(), t.copy()));
    }

    public GPNode run() throws Exception {
        return ev.run();
    }

    public double validateAllMaps(GPNode tree, int gamesPer) throws Exception {
        return ev.validateAllMaps(tree, gamesPer);
    }

    public TreeEvolver<GPNode> engine() {
        return ev;
    }
}
