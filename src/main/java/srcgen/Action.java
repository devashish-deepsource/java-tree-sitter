package srcgen;

import mytree.TreeWrapper;

import java.util.Objects;

public abstract class Action {
    final TreeWrapper tree;

    public Action(TreeWrapper tree) {
        Objects.requireNonNull(tree);
        this.tree = tree;
    }

    abstract public void apply();
}
