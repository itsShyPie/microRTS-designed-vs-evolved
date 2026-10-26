package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class MicroManager implements Manager {
    private final ModularBot bot;

    public MicroManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (!(u.getType().canAttack && !u.getType().canHarvest)) continue;
            if (gs.getActionAssignment(u) != null) continue;

            if (bb.attack) {
                Unit target = nearestEnemy(u, p, pgs);
                if (target != null) bot.attack(u, target);
            } else {
                Unit threat = nearestThreatNearBase(u, p, pgs, bb.defenseRadius);
                if (threat != null) bot.attack(u, threat);
                else holdNearBase(u, p, pgs);
            }
        }
    }

    private Unit nearestEnemy(Unit u, Player p, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != p.getID()) {
                int d = Math.abs(e.getX() - u.getX()) + Math.abs(e.getY() - u.getY());
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private Unit nearestThreatNearBase(Unit u, Player p, PhysicalGameState pgs, int radius) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == p.getID()) continue;
            if (!nearOwnBase(e, p, pgs, radius)) continue;
            int d = Math.abs(e.getX() - u.getX()) + Math.abs(e.getY() - u.getY());
            if (best == null || d < bestD) { best = e; bestD = d; }
        }
        return best;
    }

    private boolean nearOwnBase(Unit enemy, Player p, PhysicalGameState pgs, int radius) {
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == p.getID()
                    && (b.getType().isStockpile || b.getType() == bot.baseType)) {
                if (Math.abs(b.getX() - enemy.getX()) + Math.abs(b.getY() - enemy.getY()) <= radius) return true;
            }
        }
        return false;
    }

    private void holdNearBase(Unit u, Player p, PhysicalGameState pgs) {
        Unit base = null;
        int bestD = 0;
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == p.getID()
                    && (b.getType().isStockpile || b.getType() == bot.baseType)) {
                int d = Math.abs(b.getX() - u.getX()) + Math.abs(b.getY() - u.getY());
                if (base == null || d < bestD) { base = b; bestD = d; }
            }
        }
        if (base != null && bestD > 2) {
            bot.move(u, base.getX(), base.getY());
        } else {
            bot.idle(u);
        }
    }
}
