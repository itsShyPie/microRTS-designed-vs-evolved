package ai.modular.gp;

import ai.RandomBiasedAI;
import ai.abstraction.EconomyRush;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.coac.CoacAI;
import ai.core.AI;
import mayariBot.mayari;
import rts.units.UnitTypeTable;

public final class Panel {
    private Panel() {}

    public static AI make(String name, UnitTypeTable utt) {
        switch (name) {
            case "RandomBiasedAI": return new RandomBiasedAI();
            case "WorkerRush":     return new WorkerRush(utt, new AStarPathFinding());
            case "LightRush":      return new LightRush(utt, new AStarPathFinding());
            case "HeavyRush":      return new HeavyRush(utt, new AStarPathFinding());
            case "RangedRush":     return new RangedRush(utt, new AStarPathFinding());
            case "EconomyRush":    return new EconomyRush(utt, new AStarPathFinding());
            case "EconomyLightRush": return new GNS.EconomyLightRush(utt, new AStarPathFinding());
            case "Coac":           return new CoacAI(utt);
            case "Mayari":         return new mayari(utt);
            case "Rojo":           return new ai.competition.rojobot.Rojo(utt);
            case "Droplet":        return new GNS.Droplet(utt);
            default: throw new IllegalArgumentException("Unknown opponent: " + name);
        }
    }
}
