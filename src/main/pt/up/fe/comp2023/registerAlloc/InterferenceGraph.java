package pt.up.fe.comp2023.registerAlloc;

import java.util.HashSet;
import java.util.Set;

public class InterferenceGraph {

    private final Set<RegisterNode> localVars;
    private final Set<RegisterNode> params;

    public InterferenceGraph(Set<String> nodes, Set<String> params) {
        this.localVars = new HashSet<>();
        this.params = new HashSet<>();
        for (String node: nodes) {
            this.localVars.add(new RegisterNode(node));
        }
        for (String node: params) {
            this.params.add(new RegisterNode(node));
        }
    }

    public void addEdge(RegisterNode r1, RegisterNode r2) {
        r1.addEdge(r2);
        r2.addEdge(r1);
    }

    public void removeEdge(RegisterNode r1, RegisterNode r2) {
        r1.removeEdge(r2);
        r2.removeEdge(r1);
    }

    public Set<RegisterNode> getLocalVars() {
        return localVars;
    }

    public Set<RegisterNode> getParams() {
        return params;
    }

    public int getVisibleNodesCount() {
        int count = 0;
        for (RegisterNode node: localVars) {
            if (node.isVisible()) count++;
        }
        return count;
    }
}