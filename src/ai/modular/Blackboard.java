package ai.modular;

public class Blackboard {
    public int desiredWorkers = 2;
    public int desiredBarracks = 1;

    public int attackThreshold = 2;

    public int defenseRadius = 6;
    public int maxWorkerDefenders = 3;

    public boolean enableKiting = false;

    public boolean adaptiveUnit = true;

    public boolean workerRushMirror = false;

    public boolean attack = false;
    public boolean committed = false;

    public void reset() {
        attack = false;
    }
}
