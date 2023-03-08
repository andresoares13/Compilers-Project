package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyJmmAnalysis implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SymbolTableStore table = new SymbolTableStore(jmmParserResult);
        List<Report> reports = new ArrayList<>();
        Map<String, String> config = new HashMap<>();
        JmmSemanticsResult result = new JmmSemanticsResult(jmmParserResult.getRootNode(),table,reports,config);
        return result;
    }
}
