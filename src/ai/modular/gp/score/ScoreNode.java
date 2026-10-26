package ai.modular.gp.score;

import ai.modular.gp.EvoTree;

import java.util.List;
import java.util.Random;

public class ScoreNode implements EvoTree<ScoreNode> {
    public int type;
    public int feature;
    public double value;
    public ScoreNode[] kids;

    public static int arity(int type) {
        if (type <= ScoreConfig.CONST) return 0;
        if (type == ScoreConfig.IFPOS) return 3;
        return 2;
    }

    public double eval(double[] x) {
        switch (type) {
            case ScoreConfig.FEATURE: return x[feature];
            case ScoreConfig.CONST:   return value;
            case ScoreConfig.ADD:     return kids[0].eval(x) + kids[1].eval(x);
            case ScoreConfig.SUB:     return kids[0].eval(x) - kids[1].eval(x);
            case ScoreConfig.MUL:     return kids[0].eval(x) * kids[1].eval(x);
            case ScoreConfig.DIV: {
                double d = kids[1].eval(x);
                return Math.abs(d) < 1e-6 ? 1.0 : kids[0].eval(x) / d;
            }
            case ScoreConfig.MAX:     return Math.max(kids[0].eval(x), kids[1].eval(x));
            case ScoreConfig.MIN:     return Math.min(kids[0].eval(x), kids[1].eval(x));
            case ScoreConfig.IFPOS:   return kids[0].eval(x) > 0 ? kids[1].eval(x) : kids[2].eval(x);
            default: return 0.0;
        }
    }

    public ScoreNode copy() {
        ScoreNode n = new ScoreNode();
        n.type = type;
        n.feature = feature;
        n.value = value;
        if (kids != null) {
            n.kids = new ScoreNode[kids.length];
            for (int i = 0; i < kids.length; i++) n.kids[i] = kids[i].copy();
        }
        return n;
    }

    public int size() {
        if (kids == null) return 1;
        int s = 1;
        for (ScoreNode k : kids) s += k.size();
        return s;
    }

    public int depth() {
        if (kids == null) return 1;
        int d = 0;
        for (ScoreNode k : kids) d = Math.max(d, k.depth());
        return 1 + d;
    }

    public void collect(List<ScoreNode> out) {
        out.add(this);
        if (kids != null) for (ScoreNode k : kids) k.collect(out);
    }

    public void copyFieldsFrom(ScoreNode d) {
        type = d.type;
        feature = d.feature;
        value = d.value;
        kids = d.kids;
    }

    public static ScoreNode random(Random r, int depth) {
        if (depth <= 0 || (depth < ScoreConfig.MAX_DEPTH && r.nextDouble() < 0.4)) {
            return terminal(r);
        }
        int type = ScoreConfig.FIRST_FUNC + r.nextInt(ScoreConfig.LAST_FUNC - ScoreConfig.FIRST_FUNC + 1);
        ScoreNode n = new ScoreNode();
        n.type = type;
        int a = arity(type);
        n.kids = new ScoreNode[a];
        for (int i = 0; i < a; i++) n.kids[i] = random(r, depth - 1);
        return n;
    }

    private static ScoreNode terminal(Random r) {
        ScoreNode n = new ScoreNode();
        if (r.nextBoolean()) {
            n.type = ScoreConfig.FEATURE;
            n.feature = r.nextInt(ScoreConfig.NUM_FEATURES);
        } else {
            n.type = ScoreConfig.CONST;
            n.value = (r.nextDouble() * 2 - 1) * ScoreConfig.CONST_MAX;
        }
        return n;
    }

    public void mutateInPlace(Random r, List<ScoreNode> scratch) {
        scratch.clear();
        collect(scratch);
        ScoreNode pick = scratch.get(r.nextInt(scratch.size()));
        if (pick.kids == null && r.nextBoolean()) {
            pick.copyFieldsFrom(terminal(r));
        } else {
            pick.copyFieldsFrom(random(r, 2));
        }
    }
}
