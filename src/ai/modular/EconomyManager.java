package ai.modular;

import java.util.LinkedList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class EconomyManager implements Manager {
    private final ModularBot bot;

    public EconomyManager(ModularBot bot) {
        this.bot = bot;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        List<Unit> freeWorkers = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && u.getType().canHarvest) freeWorkers.add(u);
        }
        if (freeWorkers.isEmpty()) return;

        if (bb.workerRushMirror) {
            mirrorWorkerRush(player, p, gs, pgs);
            return;
        }

        int nbases = 0, nbarracks = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (u.getType() == bot.baseType) nbases++;
            if (u.getType() == bot.barracksType) nbarracks++;
        }

        List<Integer> reserved = new LinkedList<>();
        int resourcesUsed = 0;

        if (nbases == 0 && !freeWorkers.isEmpty()
                && p.getResources() >= bot.baseType.cost + resourcesUsed) {
            Unit u = freeWorkers.remove(0);
            bot.buildIfNotAlreadyBuilding(u, bot.baseType, u.getX(), u.getY(), reserved, p, pgs);
            resourcesUsed += bot.baseType.cost;
        }

        if (nbarracks < bb.desiredBarracks && !freeWorkers.isEmpty()
                && p.getResources() >= bot.barracksType.cost + resourcesUsed) {
            Unit u = freeWorkers.remove(0);
            bot.buildIfNotAlreadyBuilding(u, bot.barracksType, u.getX(), u.getY(), reserved, p, pgs);
            resourcesUsed += bot.barracksType.cost;
        }

        List<Unit> threats = enemiesNearBase(player, pgs, bb.defenseRadius);
        if (!threats.isEmpty()) {
            int defenders = Math.min(bb.maxWorkerDefenders, freeWorkers.size());
            for (int i = 0; i < defenders; i++) {
                Unit w = freeWorkers.remove(0);
                Unit t = nearest(w, threats);
                if (t != null) bot.attack(w, t);
            }
        }

        for (Unit u : freeWorkers) {
            if (bot.getAbstractAction(u) != null) continue;
            if (gs.getActionAssignment(u) != null) continue;
            assignHarvest(u, p, pgs);
        }
    }

    private List<Unit> enemiesNearBase(int player, PhysicalGameState pgs, int radius) {
        List<Unit> bases = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && (u.getType().isStockpile || u.getType() == bot.baseType)) {
                bases.add(u);
            }
        }
        List<Unit> threats = new LinkedList<>();
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == player) continue;
            for (Unit b : bases) {
                if (Math.abs(e.getX() - b.getX()) + Math.abs(e.getY() - b.getY()) <= radius) {
                    threats.add(e);
                    break;
                }
            }
        }
        return threats;
    }

    private Unit nearest(Unit from, List<Unit> targets) {
        Unit best = null;
        int bestD = 0;
        for (Unit t : targets) {
            int d = Math.abs(t.getX() - from.getX()) + Math.abs(t.getY() - from.getY());
            if (best == null || d < bestD) { best = t; bestD = d; }
        }
        return best;
    }

    private void mirrorWorkerRush(int player, Player p, GameState gs, PhysicalGameState pgs) {
        List<Unit> free = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && u.getType().canHarvest) free.add(u);
        }
        if (free.isEmpty()) return;

        int nbases = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == player && u.getType() == bot.baseType) nbases++;
        }

        List<Integer> reserved = new LinkedList<>();
        if (nbases == 0 && p.getResources() >= bot.baseType.cost) {
            Unit u = free.remove(0);
            bot.buildIfNotAlreadyBuilding(u, bot.baseType, u.getX(), u.getY(), reserved, p, pgs);
        }

        if (!free.isEmpty()) {
            Unit h = free.remove(0);
            if (bot.getAbstractAction(h) == null && gs.getActionAssignment(h) == null) {
                assignHarvest(h, p, pgs);
            }
        }

        for (Unit u : free) {
            if (gs.getActionAssignment(u) != null) continue;
            Unit target = nearestEnemy(u, p, pgs);
            if (target != null) bot.attack(u, target);
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

    private void assignHarvest(Unit u, Player p, PhysicalGameState pgs) {
        Unit closestResource = null, closestBase = null;
        int best = 0;

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < best) { closestResource = u2; best = d; }
            }
        }
        best = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestBase == null || d < best) { closestBase = u2; best = d; }
            }
        }

        if (u.getResources() > 0) {
            if (closestBase != null) bot.harvest(u, null, closestBase);
        } else if (closestResource != null && closestBase != null) {
            bot.harvest(u, closestResource, closestBase);
        }
    }
}
