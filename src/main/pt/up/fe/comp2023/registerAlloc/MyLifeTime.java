package pt.up.fe.comp2023.registerAlloc;


import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;


public class MyLifeTime {

    private OllirResult ollirResult;
    private ArrayList<MyLifeTimeCalculator> methodFlowList;

    public MyLifeTime(OllirResult ollirResult){
        this.ollirResult = ollirResult;
    }

    public void allocate() {
        calcInOut();
        colorGraph();
        allocateRegisters();
    }



    public void calcInOut() {
        ollirResult.getOllirClass().buildCFGs();
        ArrayList<Method> methods = ollirResult.getOllirClass().getMethods();
        this.methodFlowList = new ArrayList<>();

        for (Method method: methods) {
            MyLifeTimeCalculator methodFlow = new MyLifeTimeCalculator(method, ollirResult);
            methodFlow.calcInOut();
            methodFlowList.add(methodFlow);
        }
    }

    public void colorGraph() {
        for (MyLifeTimeCalculator methodFlow: methodFlowList) {
            methodFlow.buildInterferenceGraph();
            String registers = ollirResult.getConfig().get("registerAllocation");

            methodFlow.colorInterferenceGraph(Integer.parseInt(registers));
        }
    }

    public void allocateRegisters() {
        for (MyLifeTimeCalculator methodFlow: methodFlowList) {
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



}
