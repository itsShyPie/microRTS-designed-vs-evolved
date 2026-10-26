package ai.modular.gp;

import java.util.List;
import java.util.Random;

public class GPNode implements EvoTree<GPNode> {
    public boolean leaf;
    public int action;

    public int featureLeft;
    public boolean useConst;
    public double constant;
    public int featureRight;
    public boolean greater;
    public GPNode t, f;

    public GPNode() {}

    public static GPNode leaf(int action) {
        GPNode n = new GPNode();
        n.leaf = true;
        n.action = action;
        return n;
    }

    public int evaluate(double[] x) {
        GPNode n = this;
        while (!n.leaf) {
            double lhs = x[n.featureLeft];
            double rhs = n.useConst ? n.constant : x[n.featureRight];
            boolean cond = n.greater ? (lhs > rhs) : (lhs < rhs);
            n = cond ? n.t : n.f;
        }
        return n.action;
    }

    public GPNode copy() {
        GPNode n = new GPNode();
        n.leaf = leaf;
        n.action = action;
        n.featureLeft = featureLeft;
        n.useConst = useConst;
        n.constant = constant;
        n.featureRight = featureRight;
        n.greater = greater;
        if (t != null) n.t = t.copy();
        if (f != null) n.f = f.copy();
        return n;
    }

    public int size() {
        return leaf ? 1 : 1 + t.size() + f.size();
    }

    public int depth() {
        return leaf ? 1 : 1 + Math.max(t.depth(), f.depth());
    }

    public void collect(List<GPNode> out) {
        out.add(this);
        if (!leaf) {
            t.collect(out);
            f.collect(out);
        }
    }

    public void copyFieldsFrom(GPNode d) {
        leaf = d.leaf;
        action = d.action;
        featureLeft = d.featureLeft;
        useConst = d.useConst;
        constant = d.constant;
        featureRight = d.featureRight;
        greater = d.greater;
        t = d.t;
        f = d.f;
    }

    public static GPNode random(Random r, int depth) {
        if (depth <= 0 || (depth < GPConfig.MAX_DEPTH && r.nextDouble() < 0.35)) {
            return leaf(r.nextInt(GPConfig.NUM_ACTIONS));
        }
        GPNode n = new GPNode();
        n.leaf = false;
        n.featureLeft = r.nextInt(GPConfig.NUM_FEATURES);
        n.useConst = r.nextBoolean();
        n.constant = r.nextDouble() * GPConfig.CONST_MAX;
        n.featureRight = r.nextInt(GPConfig.NUM_FEATURES);
        n.greater = r.nextBoolean();
        n.t = random(r, depth - 1);
        n.f = random(r, depth - 1);
        return n;
    }

    public void mutateInPlace(Random r, List<GPNode> scratch) {
        scratch.clear();
        collect(scratch);
        GPNode pick = scratch.get(r.nextInt(scratch.size()));
        if (pick.leaf) {
            pick.action = r.nextInt(GPConfig.NUM_ACTIONS);
        } else {
            switch (r.nextInt(4)) {
                case 0:
                    pick.featureLeft = r.nextInt(GPConfig.NUM_FEATURES);
                    break;
                case 1:
                    pick.useConst = true;
                    pick.constant = r.nextDouble() * GPConfig.CONST_MAX;
                    break;
                case 2:
                    pick.useConst = false;
                    pick.featureRight = r.nextInt(GPConfig.NUM_FEATURES);
                    break;
                default:
                    pick.greater = !pick.greater;
            }
        }
    }
}
