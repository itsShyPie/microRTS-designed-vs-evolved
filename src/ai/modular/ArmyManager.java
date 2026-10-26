package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class ArmyManager implements Manager {
    private final ModularBot bot;

    public ArmyManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        int combat = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && u.getType().canAttack && !u.getType().canHarvest) combat++;
        }

        if (combat >= bb.attackThreshold) bb.committed = true;
        bb.attack = bb.committed && combat > 0;
    }
}
