package ai.modular;

import ai.modular.gp.GPConfig;
import ai.modular.gp.GPNode;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;

public class EvolvedMicroManager implements Manager {
    private final ModularBot bot;
    private final GPNode tree;

    public EvolvedMicroManager(ModularBot bot, GPNode tree) {
        this.bot = bot;
        this.tree = tree;
    }

    @Override
    public void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb) {
        if (tree == null) return;
        double[] global = globalFeatures(player, p, pgs, gs.getTime());

        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (!(u.getType().canAttack && !u.getType().canHarvest)) continue;
            if (gs.getActionAssignment(u) != null) continue;

            int action = tree.evaluate(unitFeatures(u, player, pgs, global));
            switch (action) {
                case GPConfig.A_TRAIN_WORKER:   focusFireWeakest(u, player, pgs); break;
                case GPConfig.A_TRAIN_MILITARY: advance(u, player, pgs); break;
                case GPConfig.A_MOVE_ENEMY_BASE: advance(u, player, pgs); break;
                case GPConfig.A_BUILD_BARRACKS: retreat(u, player, pgs); break;
                case GPConfig.A_RETREAT:        retreat(u, player, pgs); break;
                case GPConfig.A_IDLE:           hold(u, player, pgs); break;
                default:                        attackNearest(u, player, pgs);
            }
        }
    }

    private void attackNearest(Unit u, int player, PhysicalGameState pgs) {
        Unit e = nearestEnemy(u, player, pgs);
        if (e != null) bot.attack(u, e); else bot.idle(u);
    }

    private void focusFireWeakest(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null;
        int range = u.getAttackRange();
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() < 0 || e.getPlayer() == player) continue;
            if (dist(u, e) > range) continue;
            if (best == null || e.getHitPoints() < best.getHitPoints()) best = e;
        }
        if (best != null) bot.attack(u, best); else attackNearest(u, player, pgs);
    }

    private void advance(Unit u, int player, PhysicalGameState pgs) {
        Unit b = nearestEnemyBuilding(u, player, pgs);
        if (b != null) {
            bot.move(u, b.getX(), b.getY());
        } else {
            attackNearest(u, player, pgs);
        }
    }

    private void retreat(Unit u, int player, PhysicalGameState pgs) {
        Unit base = nearestOwnBase(u, player, pgs);
        if (base != null) bot.move(u, base.getX(), base.getY()); else bot.idle(u);
    }

    private void hold(Unit u, int player, PhysicalGameState pgs) {
        Unit e = nearestEnemy(u, player, pgs);
        if (e != null && dist(u, e) <= 2) bot.attack(u, e); else bot.idle(u);
    }

    private double[] globalFeatures(int player, Player p, PhysicalGameState pgs, long time) {
        double[] g = new double[GPConfig.NUM_FEATURES];
        int myW = 0, myA = 0, myB = 0, enW = 0, enA = 0, enB = 0;
        for (Unit u : pgs.getUnits()) {
            boolean mine = u.getPlayer() == player;
            boolean enemy = u.getPlayer() >= 0 && u.getPlayer() != player;
            if (!mine && !enemy) continue;
            if (u.getType() == bot.barracksType) { if (mine) myB++; else enB++; }
            else if (u.getType().canHarvest) { if (mine) myW++; else enW++; }
            else if (u.getType().canAttack) { if (mine) myA++; else enA++; }
        }
        g[GPConfig.F_MY_WORKERS]  = myW;
        g[GPConfig.F_MY_ARMY]     = myA;
        g[GPConfig.F_MY_BARRACKS] = myB;
        g[GPConfig.F_MY_RES]      = p.getResources();
        g[GPConfig.F_EN_WORKERS]  = enW;
        g[GPConfig.F_EN_ARMY]     = enA;
        g[GPConfig.F_EN_BARRACKS] = enB;
        g[GPConfig.F_ARMY_DIFF]       = myA - enA;
        g[GPConfig.F_WORKER_DIFF]     = myW - enW;
        g[GPConfig.F_ENEMY_NEAR_BASE] = enemyNearBase(player, pgs) ? 1 : 0;
        g[GPConfig.F_GAME_TIME]       = time / 1000.0;
        return g;
    }

    private double[] unitFeatures(Unit u, int player, PhysicalGameState pgs, double[] global) {
        double[] x = global.clone();
        x[GPConfig.F_IS_WORKER]     = 0;
        x[GPConfig.F_IS_MILITARY]   = 1;
        x[GPConfig.F_UNIT_HP]       = u.getHitPoints();
        x[GPConfig.F_UNIT_CARRY]    = u.getResources();
        x[GPConfig.F_DIST_ENEMY]    = distTo(u, nearestEnemy(u, player, pgs));
        x[GPConfig.F_DIST_BASE]     = distTo(u, nearestOwnBase(u, player, pgs));
        x[GPConfig.F_DIST_RESOURCE] = 99;
        return x;
    }

    private Unit nearestEnemy(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null; int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != player) {
                int d = dist(u, e);
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private Unit nearestEnemyBuilding(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null; int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != player
                    && (e.getType().isStockpile || e.getType() == bot.baseType || e.getType() == bot.barracksType)) {
                int d = dist(u, e);
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private Unit nearestOwnBase(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null; int bestD = 0;
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == bot.baseType)) {
                int d = dist(u, b);
                if (best == null || d < bestD) { best = b; bestD = d; }
            }
        }
        return best;
    }

    private boolean enemyNearBase(int player, PhysicalGameState pgs) {
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == bot.baseType)) {
                for (Unit e : pgs.getUnits()) {
                    if (e.getPlayer() >= 0 && e.getPlayer() != player && dist(b, e) <= 5) return true;
                }
            }
        }
        return false;
    }

    private int dist(Unit a, Unit b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private int distTo(Unit u, Unit other) {
        return other == null ? 99 : dist(u, other);
    }
}
