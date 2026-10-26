package ai.modular.gp;

public final class GPConfig {
    private GPConfig() {}

    public static final int F_IS_WORKER   = 0;
    public static final int F_IS_MILITARY = 1;
    public static final int F_UNIT_HP     = 2;
    public static final int F_UNIT_CARRY  = 3;
    public static final int F_DIST_ENEMY  = 4;
    public static final int F_DIST_BASE   = 5;
    public static final int F_MY_WORKERS  = 6;
    public static final int F_MY_ARMY     = 7;
    public static final int F_MY_BARRACKS = 8;
    public static final int F_MY_RES      = 9;
    public static final int F_EN_WORKERS  = 10;
    public static final int F_EN_ARMY     = 11;
    public static final int F_EN_BARRACKS = 12;
    public static final int F_DIST_RESOURCE   = 13;
    public static final int F_ENEMY_NEAR_BASE = 14;
    public static final int F_ARMY_DIFF       = 15;
    public static final int F_WORKER_DIFF     = 16;
    public static final int F_GAME_TIME       = 17;
    public static final int NUM_FEATURES      = 18;

    public static final int A_HARVEST         = 0;
    public static final int A_TRAIN_WORKER    = 1;
    public static final int A_TRAIN_MILITARY  = 2;
    public static final int A_BUILD_BARRACKS  = 3;
    public static final int A_ATTACK          = 4;
    public static final int A_MOVE_ENEMY_BASE = 5;
    public static final int A_RETREAT         = 6;
    public static final int A_IDLE            = 7;
    public static final int NUM_ACTIONS       = 8;

    public static final double CONST_MAX = 20.0;
    public static final int MAX_DEPTH    = 5;
    public static final int MAX_NODES    = 80;
}
