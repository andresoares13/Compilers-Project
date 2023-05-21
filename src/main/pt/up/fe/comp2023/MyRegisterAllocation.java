package pt.up.fe.comp2023;

import org.specs.comp.ollir.Descriptor;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.registerAlloc.MyLifeTime;
import pt.up.fe.comp2023.registerAlloc.MyLifeTimeCalculator;
import pt.up.fe.comp2023.registerAlloc.MyNode;

import java.util.HashMap;


public class MyRegisterAllocation {
    public static OllirResult optimize(OllirResult ollirResult) {
        System.out.println(ollirResult.getConfig());
        OllirResult optimized = ollirResult;
        if (ollirResult.getConfig().get("registerAllocation") != null) {
            MyRegisterAllocation allocator = new MyRegisterAllocation();
            allocator.allocate(ollirResult);
        }
        
        return optimized;
    }


    public void allocate(OllirResult ollirResult) {
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
    }
}