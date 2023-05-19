package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.registerAlloc.DataFlowAnalysis;
import pt.up.fe.comp2023.registerAlloc.MyLifeTime;


public class MyRegisterAllocation {
    public static OllirResult optimize(OllirResult ollirResult) {
        System.out.println(ollirResult.getConfig());
        OllirResult optimized = ollirResult;
        if (ollirResult.getConfig().get("registerAllocation") != null) {
            /*
            DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(ollirResult);
            do {
                dataFlowAnalysis.calcInOut();
            } while (dataFlowAnalysis.eliminateDeadVars());
            dataFlowAnalysis.calcInOut();
            dataFlowAnalysis.colorGraph();
            dataFlowAnalysis.allocateRegisters();

             */


            MyLifeTime lifeTime = new MyLifeTime(ollirResult);
            lifeTime.DeadVarsDealer();
            lifeTime.allocate();

        }



        return optimized;
    }
}