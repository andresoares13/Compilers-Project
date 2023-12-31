package pt.up.fe.comp2023;

import org.specs.comp.ollir.Descriptor;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.registerAlloc.MyGraph;
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
            MyLifeTimeCalculator methodLifetime = new MyLifeTimeCalculator(ollirResult.getOllirClass().getMethods().get(i));
            methodLifetime.InOutGenerator();

            MyGraph graph = new MyGraph(methodLifetime);
            graph.initGraph();
            graph.colorGraph(registerNum,ollirResult);


            HashMap<String, Descriptor> varTable = methodLifetime.getMethod().getVarTable();

            if (varTable.get("this") != null) {
                varTable.get("this").setVirtualReg(0);
            }

            for (MyNode node: graph.localVars) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }
            for (MyNode node: graph.params) {
                varTable.get(node.name).setVirtualReg(node.getReg());
            }

        }
    }
}