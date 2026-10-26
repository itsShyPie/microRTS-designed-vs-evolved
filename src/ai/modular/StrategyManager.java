package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class StrategyManager implements Manager {
    private static final int SMALL_MAP_MAX_WIDTH = 16;

    private final ModularBot bot;
    private boolean checkedMap = false;
    private boolean smallMap = false;
    private boolean decided = false;

    public StrategyManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        if (!bb.adaptiveUnit) return;

        if (!checkedMap) {
            smallMap = pgs.getWidth() <= SMALL_MAP_MAX_WIDTH;
            if (smallMap) bb.workerRushMirror = true;
            checkedMap = true;
        }
        if (decided) return;

        boolean enemyMilitary = false;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == player) continue;
            if (e.getType() == bot.barracksType
                    || (e.getType().canAttack && !e.getType().canHarvest)) {
                enemyMilitary = true;
                break;
            }
        }

        if (enemyMilitary) {
            bb.workerRushMirror = false;
            bot.militaryType = bot.lightType;
            decided = true;
        }
    }
}
