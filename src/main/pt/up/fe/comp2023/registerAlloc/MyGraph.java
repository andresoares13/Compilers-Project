package pt.up.fe.comp2023.registerAlloc;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;

import java.util.*;

import static pt.up.fe.comp.jmm.report.Stage.OPTIMIZATION;

public class MyGraph {

    public List<MyNode> localVars = new ArrayList<>();
    public List<MyNode> params = new ArrayList<>();

    public MyGraph(List<String> nodes, List<String> params) {
        for (int i=0;i<nodes.size();i++){
            this.localVars.add(new MyNode(nodes.get(i)));
        }
        for (int i=0;i<params.size();i++){
            this.params.add(new MyNode(params.get(i)));
        }
    }

    public void newEdge(MyNode n1, MyNode n2){
        n2.addEdge(n1);
        n1.addEdge(n2);
    }

    public int getVisibleNodesCount() {
        int count = 0;
        for (MyNode node: localVars) {
            if (node.isVisible){
                count++;
            }
        }
        return count;
    }


    public void initGraph(Method method, List<Node> nodeOrder, List<Set<String>> def, List<Set<String>> out) {

        for (MyNode varX: this.localVars) {
            for (MyNode varY: this.localVars) {
                if (varX.equals(varY)) {
                    continue;
                }
                for (int index = 0; index < nodeOrder.size(); index++) {
                    if (def.get(index).contains(varX.name)
                            && out.get(index).contains(varY.name)) {
                        this.newEdge(varX, varY);
                    }
                }
            }
        }
    }


    public void colorGraph(int maxK, OllirResult ollirResult) {
        Stack<MyNode> stack = new Stack<>();
        int regs = 0;

        while (this.getVisibleNodesCount() > 0) {
            for (MyNode node: this.localVars) {
                if (!node.isVisible) continue;
                int degree = node.countVisibleNeighbors();
                if (degree < regs) {
                    node.toggleVisible();
                    stack.push(node);
                } else {
                    regs += 1;
                }
            }
        }

        if (maxK > 0 && regs > maxK) {
            ollirResult.getReports().add(new Report(ReportType.ERROR, OPTIMIZATION, -1, "Not enough registers. At least " + regs + " registers are needed."));
            return;

        }
        int startReg = 1 + this.params.size();
        while (!stack.empty()) {
            MyNode node = stack.pop();
            for (int reg = startReg; reg <= regs + startReg; reg++) {
                if (node.isAllFree(reg)) {
                    node.updateReg(reg);
                    node.toggleVisible();
                    break;
                }
            }
            /*
            if (!node.isVisible) {
                ollirResult.getReports().add(
                        new Report(
                                ReportType.ERROR,
                                OPTIMIZATION,
                                -1,
                                "Unexpected error. Register allocation failed.")
                );

                throw new RuntimeException("Unexpected error. Register allocation failed.");
            }

             */
        }

        int reg = 1;
        for (MyNode node: this.params) {
            node.updateReg(reg++);
        }
    }

}
