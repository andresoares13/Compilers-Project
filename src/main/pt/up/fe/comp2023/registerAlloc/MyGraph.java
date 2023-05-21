package pt.up.fe.comp2023.registerAlloc;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.Operand;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;

import java.util.*;

import static pt.up.fe.comp.jmm.report.Stage.OPTIMIZATION;

public class MyGraph {

    public Set<MyNode> localVars = new HashSet<>();
    public Set<MyNode> params = new HashSet<>();
    private MyLifeTimeCalculator methodLifetime;

    public MyGraph(MyLifeTimeCalculator methodLifetime) {
        this.methodLifetime = methodLifetime;
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


    public void initGraph() {
        Set<String> variables = new HashSet<>();
        Set<String> params = new HashSet<>();

        for (String variable: methodLifetime.getMethod().getVarTable().keySet()) {
            List<String> parameters = new ArrayList<>();
            List<Element> elements = methodLifetime.getMethod().getParams();
            for (int i=0;i<elements.size();i++) {
                if (elements.get(i) instanceof Operand operand) {
                    parameters.add((operand.getName()));
                }
                else{
                    parameters.add(null);
                }

            }

            if (parameters.contains(variable)) {
                params.add(variable);
            } else if (!variable.equals("this")) {
                variables.add(variable);
            }
        }

        for (MyNode varX: localVars) {
            for (MyNode varY: localVars) {
                if (varX.equals(varY)) {
                    continue;
                }
                for (int index = 0; index < methodLifetime.getNodes().size(); index++) {
                    if (methodLifetime.getDef().get(index).contains(varX.name) && methodLifetime.getOut().get(index).contains(varY.name)) {
                        newEdge(varX, varY);
                    }
                }
            }
        }

        for (String vars: variables){
            MyNode var = new MyNode(vars);
            this.localVars.add(var);
        }
        for (String param: params){
            MyNode paramNode = new MyNode(param);
            this.params.add(paramNode);
        }
    }



    public void colorGraph(int maxK, OllirResult ollirResult) {
        Stack<MyNode> stack = new Stack<>();
        int k = 0;

        while (getVisibleNodesCount() > 0) {
            for (MyNode node: localVars) {
                if (!node.isVisible) continue;
                int degree = node.countVisibleNeighbors();
                if (degree < k) {
                    node.toggleVisible();
                    stack.push(node);
                } else {
                    k += 1;
                }
            }
        }

        if (maxK > 0 && k > maxK) {
            ollirResult.getReports().add(new Report(ReportType.ERROR, OPTIMIZATION, -1, "Not enough registers. At least " + k + " registers are needed.")
            );
            return;

        }
        int startReg = 1 + params.size();
        while (!stack.empty()) {
            MyNode node = stack.pop();
            for (int reg = startReg; reg <= k + startReg; reg++) {
                if (node.isAllFree(reg)) {
                    node.updateReg(reg);
                    node.toggleVisible();
                    break;
                }
            }
            if (!node.isVisible) {
                ollirResult.getReports().add(
                        new Report(
                                ReportType.ERROR,
                                OPTIMIZATION,
                                -1,
                                "Unexpected error. Register allocation failed.")
                );

                return;
            }
        }

        int reg = 1;
        for (MyNode node: params) {
            node.updateReg(reg++);
        }
    }

}
