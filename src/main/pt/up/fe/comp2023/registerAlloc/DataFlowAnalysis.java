package pt.up.fe.comp2023.registerAlloc;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataFlowAnalysis {

    private final OllirResult ollirResult;
    private ArrayList<DataFlowAnalysisAux> methodFlowList;

    public DataFlowAnalysis(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
    }

    public void calcInOut() {
        ollirResult.getOllirClass().buildCFGs();
        ArrayList<Method> methods = ollirResult.getOllirClass().getMethods();
        this.methodFlowList = new ArrayList<>();

        for (Method method: methods) {
            DataFlowAnalysisAux methodFlow = new DataFlowAnalysisAux(method, ollirResult);
            methodFlow.calcInOut();
            methodFlowList.add(methodFlow);
        }
    }

    public void colorGraph() {
        for (DataFlowAnalysisAux methodFlow: methodFlowList) {
            methodFlow.buildInterferenceGraph();
            String registers = ollirResult.getConfig().get("registerAllocation");

            methodFlow.colorInterferenceGraph(Integer.parseInt(registers));
        }
    }

    public void allocateRegisters() {
        for (DataFlowAnalysisAux methodFlow: methodFlowList) {
            HashMap<String, Descriptor> varTable = methodFlow.getMethod().getVarTable();
            for (RegisterNode node: methodFlow.getInterferenceGraph().getLocalVars()) {
                varTable.get(node.getName()).setVirtualReg(node.getRegister());
            }
            for (RegisterNode node: methodFlow.getInterferenceGraph().getParams()) {
                varTable.get(node.getName()).setVirtualReg(node.getRegister());
            }

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }
        }

    }

    public boolean eliminateDeadVars() {
        boolean hasDeadVars = false;
        for (DataFlowAnalysisAux methodFlow: methodFlowList) {
            hasDeadVars = methodFlow.eliminateDeadVars() || hasDeadVars;
        }
        return hasDeadVars;
    }

}