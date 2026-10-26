package ai.modular.gp.score;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;

import java.util.ArrayList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class ScoreGPBot extends AIWithComputationBudget {
    private UnitTypeTable utt;
    private UnitType workerType, baseType, barracksType;
    private ScoreNode tree;

    public ScoreGPBot(UnitTypeTable utt, ScoreNode tree) {
        super(-1, -1);
        this.utt = utt;
        this.tree = tree;
        resolveTypes();
    }

    private void resolveTypes() {
        if (utt != null) {
            workerType   = utt.getUnitType("Worker");
            baseType     = utt.getUnitType("Base");
            barracksType = utt.getUnitType("Barracks");
        }
    }

    @Override
    public void reset() {}

    @Override
    public AI clone() {
        return new ScoreGPBot(utt, tree == null ? null : tree.copy());
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) {
        PlayerAction pa = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        double[] global = globalFeatures(player, p, pgs, gs.getTime());
        int budget = p.getResources();

        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) continue;
            if (gs.getUnitAction(u) != null) continue;

            List<UnitAction> legal = u.getUnitActions(gs);
            if (legal.isEmpty()) continue;

            UnitAction best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (UnitAction a : legal) {
                if (!gs.isUnitActionAllowed(u, a)) continue;
                if (a.getType() == UnitAction.TYPE_PRODUCE && a.getUnitType() != null
                        && a.getUnitType().cost > budget) continue;
                double s = (tree == null) ? 0.0 : tree.eval(features(u, a, player, pgs, global));
                if (best == null || s > bestScore) {
                    bestScore = s;
                    best = a;
                }
            }
            if (best != null) {
                if (best.getType() == UnitAction.TYPE_PRODUCE && best.getUnitType() != null) {
                    budget -= best.getUnitType().cost;
                }
                try {
                    pa.addUnitAction(u, best);
                } catch (Throwable ignore) {
                }
            }
        }
        return pa;
    }

    private double[] globalFeatures(int player, Player p, PhysicalGameState pgs, long time) {
        double[] g = new double[ScoreConfig.NUM_FEATURES];
        int myW = 0, myA = 0, myB = 0, enW = 0, enA = 0;
        for (Unit u : pgs.getUnits()) {
            boolean mine = u.getPlayer() == player;
            boolean enemy = u.getPlayer() >= 0 && u.getPlayer() != player;
            if (!mine && !enemy) continue;
            if (u.getType() == barracksType) { if (mine) myB++; }
            else if (u.getType().canHarvest) { if (mine) myW++; else enW++; }
            else if (u.getType().canAttack) { if (mine) myA++; else enA++; }
        }
        g[ScoreConfig.SA_MY_WORKERS]  = myW;
        g[ScoreConfig.SA_MY_ARMY]     = myA;
        g[ScoreConfig.SA_MY_BARRACKS] = myB;
        g[ScoreConfig.SA_MY_RES]      = p.getResources();
        g[ScoreConfig.SA_EN_WORKERS]  = enW;
        g[ScoreConfig.SA_EN_ARMY]     = enA;
        g[ScoreConfig.SA_ARMY_DIFF]       = myA - enA;
        g[ScoreConfig.SA_WORKER_DIFF]     = myW - enW;
        g[ScoreConfig.SA_ENEMY_NEAR_BASE] = enemyNearBase(player, pgs) ? 1 : 0;
        g[ScoreConfig.SA_GAME_TIME]       = time / 1000.0;
        return g;
    }

    private boolean enemyNearBase(int player, PhysicalGameState pgs) {
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == baseType)) {
                for (Unit e : pgs.getUnits()) {
                    if (e.getPlayer() >= 0 && e.getPlayer() != player
                            && manhattan(b.getX(), b.getY(), e.getX(), e.getY()) <= 5) return true;
                }
            }
        }
        return false;
    }

    private Unit nearestResource(Unit u, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit rsc : pgs.getUnits()) {
            if (rsc.getType().isResource) {
                int d = manhattan(u.getX(), u.getY(), rsc.getX(), rsc.getY());
                if (best == null || d < bestD) { best = rsc; bestD = d; }
            }
        }
        return best;
    }

    private double[] features(Unit u, UnitAction a, int player, PhysicalGameState pgs, double[] global) {
        double[] x = global.clone();
        int t = a.getType();
        x[ScoreConfig.SA_IS_MOVE]    = t == UnitAction.TYPE_MOVE ? 1 : 0;
        x[ScoreConfig.SA_IS_HARVEST] = t == UnitAction.TYPE_HARVEST ? 1 : 0;
        x[ScoreConfig.SA_IS_RETURN]  = t == UnitAction.TYPE_RETURN ? 1 : 0;
        x[ScoreConfig.SA_IS_PRODUCE] = t == UnitAction.TYPE_PRODUCE ? 1 : 0;
        x[ScoreConfig.SA_IS_ATTACK]  = t == UnitAction.TYPE_ATTACK_LOCATION ? 1 : 0;
        x[ScoreConfig.SA_IS_NONE]    = t == UnitAction.TYPE_NONE ? 1 : 0;

        UnitType put = a.getUnitType();
        x[ScoreConfig.SA_PROD_WORKER]   = (put != null && put == workerType) ? 1 : 0;
        x[ScoreConfig.SA_PROD_COMBAT]   = (put != null && put.canAttack && !put.canHarvest) ? 1 : 0;
        x[ScoreConfig.SA_PROD_BUILDING] = (put != null && (put == baseType || put == barracksType)) ? 1 : 0;

        x[ScoreConfig.SA_IS_WORKER]   = u.getType().canHarvest ? 1 : 0;
        x[ScoreConfig.SA_IS_MILITARY] = (u.getType().canAttack && !u.getType().canHarvest) ? 1 : 0;
        x[ScoreConfig.SA_UNIT_HP]     = u.getHitPoints();
        x[ScoreConfig.SA_UNIT_CARRY]  = u.getResources();

        Unit e = nearestEnemy(u, player, pgs);
        int de = (e == null) ? 99 : manhattan(u.getX(), u.getY(), e.getX(), e.getY());
        x[ScoreConfig.SA_DIST_ENEMY] = de;
        x[ScoreConfig.SA_DIST_BASE]  = distNearestOwnBase(u, player, pgs);

        double toward = 0;
        if (t == UnitAction.TYPE_MOVE && e != null) {
            int d = a.getDirection();
            if (d >= 0 && d < 4) {
                int nx = u.getX() + UnitAction.DIRECTION_OFFSET_X[d];
                int ny = u.getY() + UnitAction.DIRECTION_OFFSET_Y[d];
                if (manhattan(nx, ny, e.getX(), e.getY()) < de) toward = 1;
            }
        }
        x[ScoreConfig.SA_MOVE_TOWARD_ENEMY] = toward;

        Unit res = nearestResource(u, pgs);
        double towardRes = 0;
        if (t == UnitAction.TYPE_MOVE && res != null) {
            int d = a.getDirection();
            if (d >= 0 && d < 4) {
                int nx = u.getX() + UnitAction.DIRECTION_OFFSET_X[d];
                int ny = u.getY() + UnitAction.DIRECTION_OFFSET_Y[d];
                int dres = manhattan(u.getX(), u.getY(), res.getX(), res.getY());
                if (manhattan(nx, ny, res.getX(), res.getY()) < dres) towardRes = 1;
            }
        }
        x[ScoreConfig.SA_MOVE_TOWARD_RESOURCE] = towardRes;
        return x;
    }

    private Unit nearestEnemy(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != player) {
                int d = manhattan(u.getX(), u.getY(), e.getX(), e.getY());
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private int distNearestOwnBase(Unit u, int player, PhysicalGameState pgs) {
        int best = 99;
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == baseType)) {
                int d = manhattan(u.getX(), u.getY(), b.getX(), b.getY());
                if (d < best) best = d;
            }
        }
        return best;
    }

    private int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }
}
