package ai.modular.gp.score;

public final class ScoreConfig {
    private ScoreConfig() {}

    public static final int SA_IS_MOVE          = 0;
    public static final int SA_IS_HARVEST       = 1;
    public static final int SA_IS_RETURN        = 2;
    public static final int SA_IS_PRODUCE       = 3;
    public static final int SA_IS_ATTACK        = 4;
    public static final int SA_IS_NONE          = 5;
    public static final int SA_PROD_WORKER      = 6;
    public static final int SA_PROD_COMBAT      = 7;
    public static final int SA_PROD_BUILDING    = 8;
    public static final int SA_MOVE_TOWARD_ENEMY = 9;
    public static final int SA_IS_WORKER        = 10;
    public static final int SA_IS_MILITARY      = 11;
    public static final int SA_UNIT_HP          = 12;
    public static final int SA_UNIT_CARRY       = 13;
    public static final int SA_DIST_ENEMY       = 14;
    public static final int SA_DIST_BASE        = 15;
    public static final int SA_MY_WORKERS       = 16;
    public static final int SA_MY_ARMY          = 17;
    public static final int SA_MY_BARRACKS      = 18;
    public static final int SA_MY_RES           = 19;
    public static final int SA_EN_WORKERS       = 20;
    public static final int SA_EN_ARMY          = 21;
    public static final int SA_MOVE_TOWARD_RESOURCE = 22;
    public static final int SA_ENEMY_NEAR_BASE      = 23;
    public static final int SA_ARMY_DIFF            = 24;
    public static final int SA_WORKER_DIFF          = 25;
    public static final int SA_GAME_TIME            = 26;
    public static final int NUM_FEATURES            = 27;

    public static final int FEATURE = 0;
    public static final int CONST   = 1;
    public static final int ADD     = 2;
    public static final int SUB     = 3;
    public static final int MUL     = 4;
    public static final int DIV     = 5;
    public static final int MAX     = 6;
    public static final int MIN     = 7;
    public static final int IFPOS   = 8;
    public static final int FIRST_FUNC = 2;
    public static final int LAST_FUNC  = 8;

    public static final double CONST_MAX = 5.0;
    public static final int MAX_DEPTH    = 5;
    public static final int MAX_NODES    = 120;
}
