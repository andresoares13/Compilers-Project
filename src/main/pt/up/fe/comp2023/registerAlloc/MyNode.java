package pt.up.fe.comp2023.registerAlloc;


import java.util.ArrayList;
import java.util.List;

public class MyNode {

    public String name;
    private Integer register = -1;
    public boolean isVisible;
    private List<MyNode> edges;


    public MyNode(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
        this.isVisible = true;
    }


    public void addEdge(MyNode n) {
        edges.add(n);
    }



    public int countVisibleNeighbors() {
        int count = 0;
        for (MyNode n: edges) {
            if (n.isVisible){
                count++;
            }
        }
        return count;
    }

    public int getReg(){
        return this.register;
    }

    public void toggleVisible(){
        if (this.isVisible){
            this.isVisible = false;
        }
        else{
            this.isVisible = true;
        }
    }

    public void updateReg(int n){
        this.register = n;
    }

    public boolean isAllFree(int reg){
        boolean free = true;
        for (int i=0;i<this.edges.size();i++){
            if (this.edges.get(i).getReg() != -1 && this.edges.get(i).getReg() == reg){
                free = false;
                break;
            }
        }
        return free;
    }

}
