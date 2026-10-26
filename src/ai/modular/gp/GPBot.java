package ai.modular.gp;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class GPBot extends AbstractionLayerAI {
    protected UnitTypeTable utt;
    UnitType workerType, baseType, barracksType, lightType, rangedType;
    private GPNode tree;
    private int budget;

    public GPBot(UnitTypeTable a_utt, GPNode tree) {
        this(a_utt, new AStarPathFinding(), tree);
    }

    public GPBot(UnitTypeTable a_utt, PathFinding a_pf, GPNode tree) {
        super(a_pf);
        this.tree = tree;
        reset(a_utt);
    }

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        if (utt != null) {
            workerType   = utt.getUnitType("Worker");
            baseType     = utt.getUnitType("Base");
            barracksType = utt.getUnitType("Barracks");
            lightType    = utt.getUnitType("Light");
            rangedType   = utt.getUnitType("Ranged");
        }
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public AI clone() {
        return new GPBot(utt, pf, tree == null ? null : tree.copy());
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);

        if (tree != null) {
            double[] global = globalFeatures(player, p, pgs, gs.getTime());
            budget = p.getResources();
            for (Unit u : pgs.getUnits()) {
                if (u.getPlayer() != player) continue;
                if (gs.getActionAssignment(u) != null) continue;
                double[] x = unitFeatures(u, player, pgs, global);
                execute(u, tree.evaluate(x), player, p, pgs);
            }
        }
        return translateActions(player, gs);
    }

    private double[] globalFeatures(int player, Player p, PhysicalGameState pgs, long time) {
        double[] g = new double[GPConfig.NUM_FEATURES];
        int myW = 0, myA = 0, myB = 0, enW = 0, enA = 0, enB = 0;
        for (Unit u : pgs.getUnits()) {
            boolean mine = u.getPlayer() == player;
            boolean enemy = u.getPlayer() >= 0 && u.getPlayer() != player;
            if (!mine && !enemy) continue;
            if (u.getType() == barracksType) {
                if (mine) myB++; else enB++;
            } else if (u.getType().canHarvest) {
                if (mine) myW++; else enW++;
            } else if (u.getType().canAttack) {
                if (mine) myA++; else enA++;
            }
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

    private boolean enemyNearBase(int player, PhysicalGameState pgs) {
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == baseType)) {
                for (Unit e : pgs.getUnits()) {
                    if (e.getPlayer() >= 0 && e.getPlayer() != player && manhattan(b, e) <= 5) return true;
                }
            }
        }
        return false;
    }

    private double[] unitFeatures(Unit u, int player, PhysicalGameState pgs, double[] global) {
        double[] x = global.clone();
        x[GPConfig.F_IS_WORKER]   = u.getType().canHarvest ? 1 : 0;
        x[GPConfig.F_IS_MILITARY] = (u.getType().canAttack && !u.getType().canHarvest) ? 1 : 0;
        x[GPConfig.F_UNIT_HP]     = u.getHitPoints();
        x[GPConfig.F_UNIT_CARRY]  = u.getResources();
        x[GPConfig.F_DIST_ENEMY]  = distToNearestEnemy(u, player, pgs);
        x[GPConfig.F_DIST_BASE]   = distToNearestOwnBase(u, player, pgs);
        x[GPConfig.F_DIST_RESOURCE] = distToNearestResource(u, pgs);
        return x;
    }

    private int distToNearestResource(Unit u, PhysicalGameState pgs) {
        int best = 99;
        for (Unit rsc : pgs.getUnits()) {
            if (rsc.getType().isResource) {
                int d = manhattan(rsc, u);
                if (d < best) best = d;
            }
        }
        return best;
    }

    private void execute(Unit u, int action, int player, Player p, PhysicalGameState pgs) {
        boolean isWorker   = u.getType().canHarvest;
        boolean isMilitary = u.getType().canAttack && !u.getType().canHarvest;
        boolean isBase     = u.getType() == baseType;
        boolean isBarracks = u.getType() == barracksType;

        switch (action) {
            case GPConfig.A_HARVEST:
                if (isWorker) doHarvest(u, p, pgs); else idle(u);
                break;
            case GPConfig.A_TRAIN_WORKER:
                if (isBase && budget >= workerType.cost) { train(u, workerType); budget -= workerType.cost; }
                else idle(u);
                break;
            case GPConfig.A_TRAIN_MILITARY:
                if (isBarracks) {
                    UnitType mt = (rangedType != null) ? rangedType : lightType;
                    if (mt != null && budget >= mt.cost) { train(u, mt); budget -= mt.cost; } else idle(u);
                } else idle(u);
                break;
            case GPConfig.A_BUILD_BARRACKS:
                if (isWorker && budget >= barracksType.cost) {
                    buildIfNotAlreadyBuilding(u, barracksType, u.getX(), u.getY(), new LinkedList<Integer>(), p, pgs);
                    budget -= barracksType.cost;
                } else idle(u);
                break;
            case GPConfig.A_ATTACK:
                if (isWorker || isMilitary) {
                    Unit e = nearestEnemy(u, player, pgs);
                    if (e != null) attack(u, e); else idle(u);
                } else idle(u);
                break;
            case GPConfig.A_MOVE_ENEMY_BASE:
                if (isWorker || isMilitary) {
                    Unit b = nearestEnemyBuilding(u, player, pgs);
                    if (b != null) {
                        move(u, b.getX(), b.getY());
                    } else {
                        Unit e = nearestEnemy(u, player, pgs);
                        if (e != null) attack(u, e); else idle(u);
                    }
                } else idle(u);
                break;
            case GPConfig.A_RETREAT:
                if (isWorker || isMilitary) {
                    Unit b = nearestOwnBase(u, player, pgs);
                    if (b != null) move(u, b.getX(), b.getY()); else idle(u);
                } else idle(u);
                break;
            default:
                idle(u);
        }
    }

    private void doHarvest(Unit u, Player p, PhysicalGameState pgs) {
        if (getAbstractAction(u) != null) return;
        Unit resource = null, base = null;
        int best = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                int d = manhattan(u2, u);
                if (resource == null || d < best) { resource = u2; best = d; }
            }
        }
        best = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                int d = manhattan(u2, u);
                if (base == null || d < best) { base = u2; best = d; }
            }
        }
        if (u.getResources() > 0) {
            if (base != null) harvest(u, null, base);
        } else if (resource != null && base != null) {
            harvest(u, resource, base);
        }
    }

    private Unit nearestEnemy(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != player) {
                int d = manhattan(e, u);
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private Unit nearestEnemyBuilding(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit e : pgs.getUnits()) {
            if (e.getPlayer() >= 0 && e.getPlayer() != player
                    && (e.getType().isStockpile || e.getType() == baseType || e.getType() == barracksType)) {
                int d = manhattan(e, u);
                if (best == null || d < bestD) { best = e; bestD = d; }
            }
        }
        return best;
    }

    private Unit nearestOwnBase(Unit u, int player, PhysicalGameState pgs) {
        Unit best = null;
        int bestD = 0;
        for (Unit b : pgs.getUnits()) {
            if (b.getPlayer() == player && (b.getType().isStockpile || b.getType() == baseType)) {
                int d = manhattan(b, u);
                if (best == null || d < bestD) { best = b; bestD = d; }
            }
        }
        return best;
    }

    private int distToNearestEnemy(Unit u, int player, PhysicalGameState pgs) {
        Unit e = nearestEnemy(u, player, pgs);
        return e == null ? 99 : manhattan(e, u);
    }

    private int distToNearestOwnBase(Unit u, int player, PhysicalGameState pgs) {
        Unit b = nearestOwnBase(u, player, pgs);
        return b == null ? 99 : manhattan(b, u);
    }

    private int manhattan(Unit a, Unit b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }
}
