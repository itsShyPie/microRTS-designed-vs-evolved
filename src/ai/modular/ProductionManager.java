package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitType;

public class ProductionManager implements Manager {
    private final ModularBot bot;

    public ProductionManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        int nworkers = count(pgs, player, bot.workerType);

        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player || gs.getActionAssignment(u) != null) continue;

            if (u.getType() == bot.baseType) {
                boolean wantWorker = bb.workerRushMirror
                        ? (p.getResources() >= bot.workerType.cost)
                        : (nworkers < bb.desiredWorkers && p.getResources() >= bot.workerType.cost);
                if (wantWorker) bot.train(u, bot.workerType);
            } else if (u.getType() == bot.barracksType) {
                if (p.getResources() >= bot.militaryType.cost) {
                    bot.train(u, bot.militaryType);
                }
            }
        }
    }

    private int count(PhysicalGameState pgs, int player, UnitType t) {
        int n = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && u.getType() == t) n++;
        }
        return n;
    }
}
