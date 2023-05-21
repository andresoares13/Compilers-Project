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
        ollirResult.getOllirClass().buildCFGs();
        Integer registerNum = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));
        for (int i=0;i<ollirResult.getOllirClass().getMethods().size();i++){
            MyLifeTimeCalculator methodLifetime = new MyLifeTimeCalculator(ollirResult.getOllirClass().getMethods().get(i),ollirResult);
            methodLifetime.calcInOut();
            methodLifetime.buildInterferenceGraph();
            methodLifetime.colorInterferenceGraph(registerNum);
            HashMap<String, Descriptor> varTable = methodLifetime.getMethod().getVarTable();
            for (MyNode node: methodLifetime.getInterferenceGraph().localVars) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }
            for (MyNode node: methodLifetime.getInterferenceGraph().params) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }
        }

        /*
        calcInOut();
        colorGraph();
        allocateRegisters();

         */
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
            for (MyNode node: methodFlow.getInterferenceGraph().localVars) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }
            for (MyNode node: methodFlow.getInterferenceGraph().params) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }
        }

    }



}
