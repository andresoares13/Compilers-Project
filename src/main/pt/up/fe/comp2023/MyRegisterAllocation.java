package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.registerAlloc.DataFlowAnalysis;

import static java.lang.Integer.parseInt;

public class MyRegisterAllocation {
    public static OllirResult optimize(OllirResult ollirResult) {
        System.out.println(ollirResult.getConfig());
        OllirResult optimized = ollirResult;
        if (ollirResult.getConfig().get("registerAllocation") != null) {
            int numRegs = parseInt(ollirResult.getConfig().get("registerAllocation"));
            //optimized= allocate(ollirResult, numRegs);
            DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(ollirResult);
            do {
                dataFlowAnalysis.calcInOut();
            } while (dataFlowAnalysis.eliminateDeadVars());
            dataFlowAnalysis.calcInOut();
            dataFlowAnalysis.colorGraph();
            dataFlowAnalysis.allocateRegisters();
        }

        return optimized;
    }
}