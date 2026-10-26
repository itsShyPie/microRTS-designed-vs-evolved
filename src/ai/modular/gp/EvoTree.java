package ai.modular.gp;

import java.util.List;
import java.util.Random;

public interface EvoTree<N extends EvoTree<N>> {
    N copy();
    int size();
    int depth();
    void collect(List<N> out);
    void copyFieldsFrom(N donor);
    void mutateInPlace(Random r, List<N> scratch);
}
