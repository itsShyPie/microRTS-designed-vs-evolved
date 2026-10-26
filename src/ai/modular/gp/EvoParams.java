package ai.modular.gp;

import java.io.PrintWriter;

public class EvoParams {
    public int popSize       = 50;
    public int generations   = 30;
    public int tournament    = 3;
    public double crossoverRate = 0.85;
    public double mutationRate  = 0.25;
    public double eliteFrac     = 0.1;

    public int initDepth;
    public int maxDepth = Integer.MAX_VALUE;
    public int maxNodes = Integer.MAX_VALUE;
    public boolean rejectToParent = false;

    public int maxCycles = 3000;

    public String[] trainOpponents = { "WorkerRush", "LightRush", "RandomBiasedAI" };
    public int[] fitnessGames      = { 8, 4, 4 };

    public int finalCandidates      = 6;
    public int finalValidationGames = 4;

    public int threads = 1;
    public PrintWriter genLog;

    public static EvoParams v1(int initDepth, int maxNodes) {
        EvoParams p = new EvoParams();
        p.initDepth = initDepth;
        p.maxNodes = maxNodes;
        return p;
    }
}
