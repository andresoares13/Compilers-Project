package pt.up.fe.comp2023.registerAlloc;

import java.util.ArrayList;
import java.util.Objects;

public class RegisterNode {
    private final String name;
    private Integer register;
    private boolean isVisible;
    private final ArrayList<RegisterNode> edges;


    public RegisterNode(String name) {
        this.name = name;
        this.register = null;
        this.edges = new ArrayList<>();
        this.isVisible = true;
    }

    public void addEdge(RegisterNode r2) {
        edges.add(r2);
    }

    public void removeEdge(RegisterNode r2) {
        edges.remove(r2);
    }

    public int countVisibleNeighbors() {
        int count = 0;
        for (RegisterNode r: edges) {
            if (r.isVisible()) count++;
        }
        return count;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public ArrayList<RegisterNode> getEdges() {
        return edges;
    }

    public Integer getRegister() {
        return register;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterNode that = (RegisterNode) o;
        return Objects.equals(name, that.name);
    }

    public void setInvisible() {
        isVisible = false;
    }

    public void setVisible() {
        isVisible = true;
    }

    public void setRegister(int reg) {
        register = reg;
    }

    public boolean edgeFreeRegister(int reg) {
        for (RegisterNode r: edges) {
            if (r.getRegister() != null
                    && r.getRegister().equals(reg)) return false;
        }
        return true;
    }
}
