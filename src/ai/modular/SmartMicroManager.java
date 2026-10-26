package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class SmartMicroManager implements Manager {
    private final ModularBot bot;

    public SmartMicroManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (!(u.getType().canAttack && !u.getType().canHarvest)) continue;
            if (gs.getActionAssignment(u) != null) continue;

            boolean attacking = bb.attack;

            if (bb.enableKiting && isRanged(u)) {
                Unit adj = adjacentMelee(u, p, pgs, attacking, bb.defenseRadius);
                if (adj != null) {
                    kiteAwayFrom(u, adj, pgs);
                    continue;
                }
            }

            Unit weakest = weakestInRange(u, p, pgs, attacking, bb.defenseRadius);
            if (weakest != null) {
                bot.attack(u, weakest);
                continue;
            }

            Unit nearest = attacking
                    ? nearestEnemy(u, p, pgs)
                    : nearestThreatNearBase(u, p, pgs, bb.defenseRadius);
            if (nearest != null) bot.attack(u, nearest);
            else holdNearBase(u, p, pgs);
        }
    }

    private Unit weakestInRange(Unit u, Player p, PhysicalGameState pgs, boolean attacking, int radius) {
        Unit best = null;
        int range = u.getAttackRange();
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == p.getID()) continue;
            if (!attacking && !nearOwnBase(e, p, pgs, radius)) continue;
            if (dist(u, e) > range) continue;
            if (best == null || e.getHitPoints() < best.getHitPoints()) best = e;
        }
        return best;
    }

    private Unit nearestEnemy(Unit u, Player p, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != p.getID()) {
                int d = dist(u, e);
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
            int d = dist(u, e);
            if (best == null || d < bestD) { best = e; bestD = d; }
        }
        return best;
    }

    private boolean isRanged(Unit u) {
        return u.getAttackRange() > 1;
    }

    private Unit adjacentMelee(Unit u, Player p, PhysicalGameState pgs, boolean attacking, int radius) {
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == p.getID()) continue;
            if (!attacking && !nearOwnBase(e, p, pgs, radius)) continue;
            if (e.getAttackRange() <= 1 && dist(u, e) <= 1) return e;
        }
        return null;
    }

    private void kiteAwayFrom(Unit u, Unit e, PhysicalGameState pgs) {
        int dx = Integer.signum(u.getX() - e.getX());
        int dy = Integer.signum(u.getY() - e.getY());
        int tx = clamp(u.getX() + dx, 0, pgs.getWidth() - 1);
        int ty = clamp(u.getY() + dy, 0, pgs.getHeight() - 1);
        if (tx == u.getX() && ty == u.getY()) {
            bot.attack(u, e);
        } else {
            bot.move(u, tx, ty);
        }
    }

    private boolean nearOwnBase(Unit enemy, Player p, PhysicalGameState pgs, int radius) {
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == p.getID()
                    && (b.getType().isStockpile || b.getType() == bot.baseType)) {
                if (dist(b, enemy) <= radius) return true;
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
                int d = dist(b, u);
                if (base == null || d < bestD) { base = b; bestD = d; }
            }
        }
        if (base != null && bestD > 2) bot.move(u, base.getX(), base.getY());
        else bot.idle(u);
    }

    private int dist(Unit a, Unit b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
