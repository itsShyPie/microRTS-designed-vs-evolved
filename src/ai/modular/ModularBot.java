package ai.modular;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.modular.gp.GPNode;

import java.util.ArrayList;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class ModularBot extends AbstractionLayerAI {
    protected UnitTypeTable utt;
    UnitType workerType, baseType, barracksType, lightType, heavyType, rangedType;
    String militaryTypeName = "Ranged";
    UnitType militaryType;

    boolean smartMicro = true;
    GPNode combatTree;
    Blackboard bb = new Blackboard();
    List<Manager> managers = new ArrayList<>();

    public ModularBot(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding(), true);
    }

    public ModularBot(UnitTypeTable a_utt, PathFinding a_pf) {
        this(a_utt, a_pf, true);
    }

    public ModularBot(UnitTypeTable a_utt, PathFinding a_pf, boolean smartMicro) {
        super(a_pf);
        this.smartMicro = smartMicro;
        reset(a_utt);
    }

    public ModularBot(UnitTypeTable a_utt, PathFinding a_pf, boolean smartMicro, GPNode combatTree) {
        super(a_pf);
        this.smartMicro = smartMicro;
        this.combatTree = combatTree;
        reset(a_utt);
    }

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        if (utt != null) {
            workerType   = utt.getUnitType("Worker");
            baseType     = utt.getUnitType("Base");
            barracksType = utt.getUnitType("Barracks");
            lightType    = utt.getUnitType("Light");
            heavyType    = utt.getUnitType("Heavy");
            rangedType   = utt.getUnitType("Ranged");
            militaryType = utt.getUnitType(militaryTypeName);
        }
        managers.clear();
        managers.add(new StrategyManager(this));
        managers.add(new ProductionManager(this));
        managers.add(new EconomyManager(this));
        managers.add(new ArmyManager(this));
        if (combatTree != null) {
            managers.add(new EvolvedMicroManager(this, combatTree));
        } else {
            managers.add(smartMicro ? new SmartMicroManager(this) : new MicroManager(this));
        }
    }

    @Override
    public void reset() {
        super.reset();
    }

    public void configure(String milType, int desiredWorkers, int desiredBarracks) {
        this.militaryTypeName = milType;
        if (utt != null) militaryType = utt.getUnitType(milType);
        bb.desiredWorkers = desiredWorkers;
        bb.desiredBarracks = desiredBarracks;
        bb.adaptiveUnit = false;
    }

    @Override
    public AI clone() {
        return new ModularBot(utt, pf, smartMicro, combatTree == null ? null : combatTree.copy());
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);

        bb.reset();
        for (Manager m : managers) {
            m.act(player, p, gs, pgs, bb);
        }

        return translateActions(player, gs);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        return parameters;
    }
}
