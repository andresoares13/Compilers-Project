package pt.up.fe.comp2023;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.Operand;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.visitors.MyGraphColoring;
import pt.up.fe.comp2023.visitors.MyLifetimeAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Integer.parseInt;

public class MyRegisterAllocation {
    public static OllirResult optimize(OllirResult ollirResult){
        System.out.println(ollirResult.getConfig());
        OllirResult optimized = ollirResult;
        if (ollirResult.getConfig().get("registerAllocation") != null){
            int numRegs = parseInt(ollirResult.getConfig().get("registerAllocation"));;
            optimized= allocate(ollirResult, numRegs);
        }

        return optimized;
    }

    public static OllirResult allocate(OllirResult ollirResult, int numRegs){
        MyLifetimeAnalysis lifetime = new MyLifetimeAnalysis();
        List<Report> reportsList = ollirResult.getReports();
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            ArrayList<HashMap<Node, ArrayList<Operand>>> liveRanges = lifetime.analyze(method);
            MyGraphColoring intGraph = new MyGraphColoring(liveRanges, method, reportsList);

            HashMap<String, Descriptor> v2Table = intGraph.graphColoring(numRegs);

            if (v2Table != null) {
                for (var entry : method.getVarTable().entrySet()) {
                    for (var entry2 : v2Table.entrySet()) {
                        if (entry.getKey().equals(entry2.getKey())) {
                            method.getVarTable().replace(entry.getKey(), entry.getValue(), entry2.getValue());
                        }
                        if (!v2Table.containsKey(entry.getKey())) {
                            method.getVarTable().remove(entry.getKey());
                        }
                    }
                }

            }
        }
        return ollirResult;
    }
}
