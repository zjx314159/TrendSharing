package edu.ecnu.hwu.hst;

import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ToString(exclude = {"children", "parent"})
public class TrendNode {
    public TrendNode parent;
    public List<TrendNode> children;

    public Set<Integer> tids;

    public int eid;
    public int level;
    public int pNid;
    public int dNid;

    public TrendNode(Set<Integer> tids, int eid, int level, int pNid, int dNid) {
        this.tids = new HashSet<>(tids);
        this.eid = eid;
        this.level = level;
        this.pNid = pNid;
        this.dNid = dNid;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public void addChild(TrendNode child) {
        children.add(child);
    }

    public int getWei() {
        return tids.size();
    }
}
